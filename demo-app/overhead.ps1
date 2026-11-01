# T1/T2 measurement (spike-acceptance.md): demo load, agent off vs on, warmed
# JVM, median of 5 runs. Also reports trace volume (events/min, event-file
# MB/min, and stored-SQLite MB/min after collector ingest) for the agent-on
# phase. The load is server-saturating (parallel clients) — the S0-4
# sequential shape could not resolve agent overhead (ratio 0.87x, variance
# above any plausible agent cost) and is superseded by this method, recorded
# here before the gate judgement (Step 5, 2026-11-01).
#
# Method (frozen for this measurement): ABBA phase order (off, on, on, off)
# with a fresh app JVM per phase, cancelling the linear warmup/drift trend
# that swamped two earlier attempts (sequential S0-4: 0.87x; parallel
# two-phase: 1.05x with monotonic run-to-run speedup in both phases).
# Per phase: warmup of 2 x $Requests requests, then 5 runs x $Requests
# requests at $Threads parallel clients,
# median wall-clock per phase (T1 ratio = median on / median off), capture
# profile allow=trace.demoapp, summary level (ADR-0014 event-file path).
# T2 volume = stored DB bytes after ingesting the whole agent-on event
# file, divided by the summed measured run time of the agent-on phase.
#
# Usage: pwsh demo-app/overhead.ps1 [-Requests 1500] [-Runs 5] [-Threads 16]
# (from repo root; requires :demo-app:bootJar, :agent:shadowJar, and compiled
# LoadClient via :demo-app:compileJava)
param([int]$Requests = 1500, [int]$Runs = 5, [int]$Port = 18080, [int]$Threads = 16)
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$demoJar = (Get-ChildItem "$root/demo-app/build/libs/*.jar" | Where-Object Name -NotMatch "plain" | Select-Object -First 1).FullName
$agentJar = "$root/agent/build/libs/agent.jar"
$loadCp = "$root/demo-app/build/classes/java/main"
$events = "$env:TEMP/trace-overhead-events.jsonl"
$script:PhaseTotalMs = 0
$script:PhaseEventsBefore = 0
$script:PhaseBytesBefore = 0

function Run-Phase([string]$label, [string[]]$jvmArgs) {
    Remove-Item $events -ErrorAction SilentlyContinue
    $proc = Start-Process java -ArgumentList ($jvmArgs + @("-jar", $demoJar, "--server.port=$Port")) -PassThru -NoNewWindow `
        -RedirectStandardOutput "$env:TEMP/demo-$label.out" -RedirectStandardError "$env:TEMP/demo-$label.err"
    try {
        $deadline = (Get-Date).AddSeconds(90)
        do {
            Start-Sleep -Milliseconds 500
            try { $ok = (Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 "http://localhost:$Port/stock/SKU-1").StatusCode -eq 200 } catch { $ok = $false }
        } while (-not $ok -and (Get-Date) -lt $deadline)
        if (-not $ok) { throw "demo app did not start ($label): $(Get-Content "$env:TEMP/demo-$label.err" -Tail 5)" }
        1..2 | ForEach-Object { java -cp $loadCp trace.demoapp.LoadClient "http://localhost:$Port" $Requests $Threads | Out-Null }  # warmup
        # Volume baseline: warmup events are excluded from the T2 numerator.
        if (Test-Path $events) {
            $script:PhaseEventsBefore = (Get-Content $events | Measure-Object).Count
            $script:PhaseBytesBefore = (Get-Item $events).Length
        }
        $times = @()
        for ($i = 0; $i -lt $Runs; $i++) {
            $line = java -cp $loadCp trace.demoapp.LoadClient "http://localhost:$Port" $Requests $Threads
            $times += [long]([regex]::Match($line, "elapsed_ms=(\d+)").Groups[1].Value)
            Write-Host "$label run $($i+1): $($times[-1]) ms"
        }
        $script:PhaseTotalMs = ($times | Measure-Object -Sum).Sum
        $sorted = $times | Sort-Object
        return $sorted[[int]($sorted.Count / 2)]
    } finally { Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue; $proc.WaitForExit() }
}

$eventsVol = "$env:TEMP/trace-overhead-vol.jsonl"
$offMedians = @(); $onMedians = @()
$volMinutes = 0.0; $volEventCount = 0; $volEventMb = 0.0
foreach ($phase in @("off", "on", "on", "off")) {
    $label = "$phase-$([guid]::NewGuid().ToString('N').Substring(0,6))"
    if ($phase -eq "off") {
        $offMedians += Run-Phase $label @()
    } else {
        $onMedians += Run-Phase $label @("-javaagent:$agentJar=allow=trace.demoapp;out=$events")
        if ($volEventCount -eq 0) {
            # Keep the first agent-on phase for the T2 volume accounting.
            Copy-Item $events $eventsVol -Force
            $volMinutes = $script:PhaseTotalMs / 60000.0
            $volEventCount = (Get-Content $events | Measure-Object).Count - $script:PhaseEventsBefore
            $volEventMb = ((Get-Item $events).Length - $script:PhaseBytesBefore) / 1MB
        }
    }
}
$offSorted = $offMedians | Sort-Object; $onSorted = $onMedians | Sort-Object
$off = ($offSorted | Measure-Object -Sum).Sum / $offSorted.Count
$on = ($onSorted | Measure-Object -Sum).Sum / $onSorted.Count
Write-Host "RESULT phase_medians_off=$($offMedians -join ',') phase_medians_on=$($onMedians -join ',')"
Write-Host "RESULT mean_phase_median_off_ms=$([math]::Round($off)) mean_phase_median_on_ms=$([math]::Round($on)) ratio=$([math]::Round($on / $off, 2))"
Write-Host "RESULT events_measured=$volEventCount event_file_mb=$([math]::Round($volEventMb, 3)) on_benchmark_min=$([math]::Round($volMinutes, 3)) event_file_mb_per_min=$([math]::Round($volEventMb / $volMinutes, 2)) events_per_min=$([math]::Round($volEventCount / $volMinutes))"

# T2 stored volume: ingest the kept agent-on event file via the collector and
# size the DB, scaled by the measured-runs share of its events.
$db = "$env:TEMP/trace-overhead.db"
Remove-Item $db -ErrorAction SilentlyContinue
& "$root\gradlew.bat" :collector:run "--args=$eventsVol $db demo-app" | Select-Object -Last 2
$totalEvents = (Get-Content $eventsVol | Measure-Object).Count
$dbMb = (Get-Item $db).Length / 1MB * $volEventCount / $totalEvents
Write-Host "RESULT stored_db_mb_measured_share=$([math]::Round($dbMb, 3)) stored_mb_per_min=$([math]::Round($dbMb / $volMinutes, 2))"

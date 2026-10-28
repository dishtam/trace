# S0-4 overhead smoke (spike-acceptance.md): demo load, agent off vs on, warmed JVM,
# median of 5 runs. Also reports trace volume (MB/min, events/min) for the agent-on runs.
# Usage: pwsh demo-app/overhead.ps1   (from repo root; requires :demo-app:bootJar and :agent:shadowJar built)
param([int]$Requests = 100, [int]$Runs = 5, [int]$Port = 18080)
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$demoJar = (Get-ChildItem "$root/demo-app/build/libs/*.jar" | Where-Object Name -NotMatch "plain" | Select-Object -First 1).FullName
$agentJar = "$root/agent/build/libs/agent.jar"
$loadCp = "$root/demo-app/build/classes/java/main"
$events = "$env:TEMP/trace-overhead-events.jsonl"

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
        java -cp $loadCp trace.demoapp.LoadClient "http://localhost:$Port" 30 | Out-Null  # warmup
        $times = @()
        for ($i = 0; $i -lt $Runs; $i++) {
            $line = java -cp $loadCp trace.demoapp.LoadClient "http://localhost:$Port" $Requests
            $times += [long]([regex]::Match($line, "elapsed_ms=(\d+)").Groups[1].Value)
            Write-Host "$label run $($i+1): $($times[-1]) ms"
        }
        $sorted = $times | Sort-Object
        return $sorted[[int]($sorted.Count / 2)]
    } finally { Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue; $proc.WaitForExit() }
}

$off = Run-Phase "off" @()
$on = Run-Phase "on" @("-javaagent:$agentJar=allow=trace.demoapp;out=$events")
$eventCount = (Get-Content $events | Measure-Object).Count
$eventMb = (Get-Item $events).Length / 1MB
Write-Host "RESULT median_off_ms=$off median_on_ms=$on ratio=$([math]::Round($on / $off, 2))"
Write-Host "RESULT events=$eventCount event_file_mb=$([math]::Round($eventMb, 3))"

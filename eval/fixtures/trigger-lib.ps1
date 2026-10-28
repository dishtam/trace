# Shared helpers for fixture trigger scripts. Each trigger starts a FRESH demo
# app (own port, optional Spring profile), runs its requests, and stops the app.
# A trigger exits 0 when the fixture's expected symptom is observed.
$DemoJar = Join-Path $PSScriptRoot "..\..\demo-app\build\libs\demo-app.jar"

function Start-DemoApp([int]$Port, [string]$Profile = "") {
    $args = @("-jar", $DemoJar, "--server.port=$Port")
    if ($Profile) { $args += "--spring.profiles.active=$Profile" }
    $proc = Start-Process java -ArgumentList $args -PassThru -NoNewWindow `
        -RedirectStandardOutput "$env:TEMP\demo-$Port.out" -RedirectStandardError "$env:TEMP\demo-$Port.err"
    $deadline = (Get-Date).AddSeconds(90)
    do {
        Start-Sleep -Milliseconds 500
        try { $ok = (Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 "http://localhost:$Port/stock/SKU-1").StatusCode -eq 200 }
        catch { $ok = $false }
    } while (-not $ok -and (Get-Date) -lt $deadline)
    if (-not $ok) { throw "demo app did not start on port $Port (profile '$Profile')" }
    return $proc
}

function Stop-DemoApp($Proc) { Stop-Process -Id $Proc.Id -Force -ErrorAction SilentlyContinue; $Proc.WaitForExit() }

function Invoke-Req([string]$Method, [string]$Url, [string]$Body = $null) {
    try {
        $params = @{ UseBasicParsing = $true; Method = $Method; TimeoutSec = 15 }
        if ($Body) { $params.ContentType = "application/json"; $params.Body = $Body }
        $r = Invoke-WebRequest @params -Uri $Url
        return @{ Status = $r.StatusCode; Body = $r.Content }
    } catch {
        $status = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { 0 }
        return @{ Status = $status; Body = "" }
    }
}

function Assert-True([bool]$Cond, [string]$Message) {
    if ($Cond) { Write-Host "PASS: $Message" } else { Write-Host "FAIL: $Message"; exit 1 }
}

. "$PSScriptRoot\..\trigger-lib.ps1"
$Port = 18110
$proc = Start-DemoApp $Port "b10"
try {
$base = "http://localhost:$Port"
$r = Invoke-Req GET "$base/orders/9999"
Assert-True ($r.Status -eq 500) "B10 seeded: missing order -> HTTP 500 from web-layer NPE (got $($r.Status))"
} finally { Stop-DemoApp $proc }
Write-Host "B10 trigger: expected symptom observed"


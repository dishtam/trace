. "$PSScriptRoot\..\trigger-lib.ps1"
$Port = 18111
$proc = Start-DemoApp $Port "b11"
try {
$base = "http://localhost:$Port"
$confirm = Invoke-Req POST "$base/orders/9999/confirm"
Assert-True ($confirm.Status -eq 200) "B11 seeded: failed async confirmation reported as 200 SUBMITTED (got $($confirm.Status))"
} finally { Stop-DemoApp $proc }
Write-Host "B11 trigger: expected symptom observed"

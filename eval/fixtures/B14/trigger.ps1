. "$PSScriptRoot\..\trigger-lib.ps1"
$Port = 18114
$proc = Start-DemoApp $Port "b14"
try {
$base = "http://localhost:$Port"
$r = Invoke-Req GET "$base/pricing?sku=SKU-1&quantity=1"
Assert-True ($r.Body -match '"totalCents":1250') "B14 seeded: wrong-key tax gives 1250 (got $($r.Body))"
} finally { Stop-DemoApp $proc }
Write-Host "B14 trigger: expected symptom observed"


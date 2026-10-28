. "$PSScriptRoot\..\trigger-lib.ps1"
$Port = 18103
$proc = Start-DemoApp $Port "b03"
try {
$base = "http://localhost:$Port"
$r = Invoke-Req GET "$base/pricing?sku=SKU-1&quantity=5"
Assert-True ($r.Body -match '"totalCents":5500') "B03 seeded: threshold order overcharged to 5500 (got $($r.Body))"
} finally { Stop-DemoApp $proc }
Write-Host "B03 trigger: expected symptom observed"


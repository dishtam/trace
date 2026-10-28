. "$PSScriptRoot\..\trigger-lib.ps1"
$Port = 18109
$proc = Start-DemoApp $Port "b09"
try {
$base = "http://localhost:$Port"
$first = Invoke-Req GET "$base/pricing?sku=SKU-1&quantity=1"
$second = Invoke-Req GET "$base/pricing?sku=SKU-2&quantity=1"
Assert-True ($first.Body -match '"totalCents":1100') "B09: first call 1100 (got $($first.Body))"
Assert-True ($second.Body -match '"totalCents":1100') "B09 seeded: second call bleeds first total 1100 (got $($second.Body))"
} finally { Stop-DemoApp $proc }
Write-Host "B09 trigger: expected symptom observed"


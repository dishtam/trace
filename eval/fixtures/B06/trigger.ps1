. "$PSScriptRoot\..\trigger-lib.ps1"
$Port = 18106
$proc = Start-DemoApp $Port "b06"
try {
$base = "http://localhost:$Port"
$placed = Invoke-Req POST "$base/orders" '{"sku":"SKU-1","quantity":1}'
Assert-True ($placed.Body -match '"totalCents":0') "B06 seeded: order total is 0, last line dropped (got $($placed.Body))"
} finally { Stop-DemoApp $proc }
Write-Host "B06 trigger: expected symptom observed"

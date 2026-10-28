. "$PSScriptRoot\..\trigger-lib.ps1"
$Port = 18102
$proc = Start-DemoApp $Port "b02"
try {
$base = "http://localhost:$Port"
$r = Invoke-Req POST "$base/orders" '{"sku":"SKU-2","quantity":1}'
Assert-True ($r.Status -eq 500) "B02 seeded: zero-rated SKU-2 order -> HTTP 500 NPE (got $($r.Status))"
$good = Invoke-Req POST "$base/orders" '{"sku":"SKU-1","quantity":1}'
Assert-True ($good.Status -eq 200) "B02: SKU-1 still succeeds (got $($good.Status))"
} finally { Stop-DemoApp $proc }
Write-Host "B02 trigger: expected symptom observed"


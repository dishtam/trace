. "$PSScriptRoot\..\trigger-lib.ps1"
$Port = 18101
$proc = Start-DemoApp $Port "b01"
try {
$base = "http://localhost:$Port"
$r = Invoke-Req POST "$base/orders" '{"sku":"UNKNOWN-SKU","quantity":1}'
Assert-True ($r.Status -eq 500) "B01 seeded: unknown SKU -> HTTP 500 (got $($r.Status))"
$good = Invoke-Req POST "$base/orders" '{"sku":"SKU-1","quantity":1}'
Assert-True ($good.Status -eq 200) "B01: known SKU still succeeds (got $($good.Status))"
} finally { Stop-DemoApp $proc }
Write-Host "B01 trigger: expected symptom observed"


. "$PSScriptRoot\..\trigger-lib.ps1"
$Port = 18108
$proc = Start-DemoApp $Port "b08"
try {
$base = "http://localhost:$Port"
$before = Invoke-Req GET "$base/stock/SKU-1"
$beforeQty = [int]([regex]::Match($before.Body, '"available":(\d+)').Groups[1].Value)
$placed = Invoke-Req POST "$base/orders" '{"sku":"SKU-1","quantity":1}'
$id = [regex]::Match($placed.Body, '"id":(\d+)').Groups[1].Value
Invoke-Req POST "$base/orders/$id/cancel" | Out-Null
$after = Invoke-Req GET "$base/stock/SKU-1"
$afterQty = [int]([regex]::Match($after.Body, '"available":(\d+)').Groups[1].Value)
Assert-True ($afterQty -eq $beforeQty - 2) "B08 seeded: cancelled order decremented stock again ($beforeQty -> $afterQty)"
} finally { Stop-DemoApp $proc }
Write-Host "B08 trigger: expected symptom observed"

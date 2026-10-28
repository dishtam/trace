. "$PSScriptRoot\..\trigger-lib.ps1"
$Port = 18105
$proc = Start-DemoApp $Port "b05"
try {
$base = "http://localhost:$Port"
$placed = Invoke-Req POST "$base/orders" '{"sku":"SKU-1","quantity":1}'
$id = [regex]::Match($placed.Body, '"id":(\d+)').Groups[1].Value
$list = Invoke-Req GET "$base/orders?page=0&size=10"
Assert-True ($list.Body -notmatch "`"id`":$id") "B05 seeded: first order $id missing from page 0 (got $($list.Body))"
} finally { Stop-DemoApp $proc }
Write-Host "B05 trigger: expected symptom observed"


. "$PSScriptRoot\..\trigger-lib.ps1"
$Port = 18104
$proc = Start-DemoApp $Port "b04"
try {
$base = "http://localhost:$Port"
$stock = Invoke-Req GET "$base/stock/SKU-2"
$qty = [int]([regex]::Match($stock.Body, '"available":(\d+)').Groups[1].Value)
$r = Invoke-Req POST "$base/orders" "{`"sku`":`"SKU-2`",`"quantity`":$qty}"
Assert-True ($r.Status -eq 500) "B04 seeded: buying exactly the stock ($qty) rejected (got $($r.Status))"
} finally { Stop-DemoApp $proc }
Write-Host "B04 trigger: expected symptom observed"


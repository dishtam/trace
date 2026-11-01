. "$PSScriptRoot\..\trigger-lib.ps1"
$Port = 18107
$proc = Start-DemoApp $Port "b07"
try {
$base = "http://localhost:$Port"
Invoke-Req PUT "$base/products/SKU-1/sale-price" '{"salePriceCents":500}' | Out-Null
$view = Invoke-Req GET "$base/products/SKU-1"
Assert-True ($view.Body -match '"listPriceCents":500' -and $view.Body -match '"salePriceCents":0') "B07 seeded: sale update wrote list price, sale price unset (got $($view.Body))"
} finally { Stop-DemoApp $proc }
Write-Host "B07 trigger: expected symptom observed"

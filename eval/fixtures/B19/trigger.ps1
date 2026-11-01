. "$PSScriptRoot\..\trigger-lib.ps1"
$Port = 18119
$proc = Start-DemoApp $Port "b19"
try {
$base = "http://localhost:$Port"
$shipping = Invoke-Req GET "$base/checkout/shipping?sku=SKU-2&quantity=2"
Assert-True ($shipping.Body -match '"shippingCents":0') "B19 seeded: free shipping from pre-discount comparison (got $($shipping.Body))"
} finally { Stop-DemoApp $proc }
Write-Host "B19 trigger: expected symptom observed"

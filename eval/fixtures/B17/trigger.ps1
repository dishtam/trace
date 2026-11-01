. "$PSScriptRoot\..\trigger-lib.ps1"
$Port = 18117
$proc = Start-DemoApp $Port "b17"
try {
$base = "http://localhost:$Port"
$callback = Invoke-Req POST "$base/pricing/callback" '{"sku":"SKU-1","totalCents":1234}'
Assert-True ($callback.Body -match '"acceptedTotalCents":0') "B17 seeded: callback total lost by field rename (got $($callback.Body))"
} finally { Stop-DemoApp $proc }
Write-Host "B17 trigger: expected symptom observed"

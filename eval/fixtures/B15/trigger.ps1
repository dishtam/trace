. "$PSScriptRoot\..\trigger-lib.ps1"
$Port = 18115
$proc = Start-DemoApp $Port "b15"
try {
$base = "http://localhost:$Port"
$price = Invoke-Req GET "$base/pricing?sku=SKU-1&quantity=1"
Assert-True ($price.Body -match '"totalCents":990') "B15 seeded: discount applied below threshold, profile policy unconditional (got $($price.Body))"
} finally { Stop-DemoApp $proc }
Write-Host "B15 trigger: expected symptom observed"

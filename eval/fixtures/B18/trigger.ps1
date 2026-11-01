. "$PSScriptRoot\..\trigger-lib.ps1"
$Port = 18118
$proc = Start-DemoApp $Port "b18"
try {
$base = "http://localhost:$Port"
$regional = Invoke-Req GET "$base/pricing/regional?sku=SKU-1&quantity=1&region=EU"
Assert-True ($regional.Status -eq 500) "B18 seeded: regional pricing NPEs on wrong map key (got $($regional.Status))"
} finally { Stop-DemoApp $proc }
Write-Host "B18 trigger: expected symptom observed"

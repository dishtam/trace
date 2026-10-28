. "$PSScriptRoot\..\trigger-lib.ps1"
$Port = 18116
$proc = Start-DemoApp $Port "b16"
try {
$base = "http://localhost:$Port"
$r = Invoke-Req POST "$base/orders" '{"sku":"SKU-1","quantity":1}'
Assert-True ($r.Status -eq 200) "B16 seeded: order succeeds (got $($r.Status))"
Assert-True ($r.Body -notmatch 'totalCents') "B16 seeded: response drops totalCents (got $($r.Body))"
} finally { Stop-DemoApp $proc }
Write-Host "B16 trigger: expected symptom observed"


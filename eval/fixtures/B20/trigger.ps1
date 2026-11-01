. "$PSScriptRoot\..\trigger-lib.ps1"
$Port = 18120
$proc = Start-DemoApp $Port "b20"
try {
$base = "http://localhost:$Port"
$order = Invoke-Req POST "$base/orders" '{"sku":"UNKNOWN","quantity":1}'
Assert-True ($order.Status -eq 500) "B20 seeded: unknown-sku order fails (got $($order.Status))"
} finally { Stop-DemoApp $proc }
$log = Get-Content "$env:TEMP\demo-$Port.out" -Raw
Assert-True ($log -match 'checkout failed') "B20 seeded: bare 'checkout failed' exception logged"
Assert-True ($log -notmatch 'unknown sku') "B20 seeded: original cause (unknown sku) absent from log, chain replaced"
Write-Host "B20 trigger: expected symptom observed"

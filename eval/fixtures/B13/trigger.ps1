. "$PSScriptRoot\..\trigger-lib.ps1"
$Port = 18113
$proc = Start-DemoApp $Port "b13"
try {
$base = "http://localhost:$Port"
$placed = Invoke-Req POST "$base/orders" '{"sku":"SKU-1","quantity":1}'
$id = [regex]::Match($placed.Body, '"id":(\d+)').Groups[1].Value
$status = ""
for ($i = 0; $i -lt 10; $i++) {
    $conf = Invoke-Req GET "$base/orders/$id/confirmation"
    $status = [regex]::Match($conf.Body, '"status":"(\w+)"').Groups[1].Value
    if ($status -ne "NONE") { break }
    Start-Sleep -Milliseconds 500
}
Assert-True ($status -eq "NONE") "B13 seeded: order $id never confirmed, context lost in hand-off (status $status)"
} finally { Stop-DemoApp $proc }
Write-Host "B13 trigger: expected symptom observed"

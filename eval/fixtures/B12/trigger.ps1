. "$PSScriptRoot\..\trigger-lib.ps1"
$Port = 18112
$proc = Start-DemoApp $Port "b12"
try {
$base = "http://localhost:$Port"
Invoke-Req PUT "$base/stock/SKU-1" '{"quantity":1}' | Out-Null
$jobs = 1..2 | ForEach-Object {
    Start-Job -ScriptBlock {
        param($Base)
        try {
            $r = Invoke-WebRequest -UseBasicParsing -Method POST -Uri "$Base/orders" -ContentType "application/json" -Body '{"sku":"SKU-1","quantity":1}' -TimeoutSec 60
            $r.StatusCode
        } catch {
            if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { 0 }
        }
    } -ArgumentList $base
}
$codes = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job
Assert-True (($codes | Where-Object { $_ -eq 200 }).Count -eq 2) "B12 seeded: both parallel orders succeeded for one unit (got $($codes -join ','))"
} finally { Stop-DemoApp $proc }
Write-Host "B12 trigger: expected symptom observed"

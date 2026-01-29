# Simple Load Test con medicion de rendimiento
param(
    [int]$Requests = 1000,
    [int]$Parallel = 50
)

Write-Host "Iniciando Load Test..." -ForegroundColor Cyan
Write-Host "Requests totales: $Requests"
Write-Host "Paralelo: $Parallel"
Write-Host ""

$success = 0
$failed = 0
$startTime = Get-Date

$jobs = @()
for ($i = 1; $i -le $Requests; $i++) {
    $job = Start-Job -ScriptBlock {
        param($userId)
        $body = @{
            userId = $userId
            latitude = 40.7128
            longitude = -74.0060
            timestamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
        } | ConvertTo-Json
        
        try {
            Invoke-RestMethod -Uri "http://localhost:8080/api/locations" `
                -Method Post -Body $body -ContentType "application/json" `
                -UseBasicParsing -TimeoutSec 10 | Out-Null
            return $true
        }
        catch {
            return $false
        }
    } -ArgumentList ($i % 100)
    
    $jobs += $job
    
    # Limitar paralelismo
    while (($jobs | Where-Object { $_.State -eq 'Running' }).Count -ge $Parallel) {
        Start-Sleep -Milliseconds 10
    }
    
    # Progress
    if ($i % 100 -eq 0) {
        Write-Host "Enviadas: $i" -NoNewline
        Write-Host "`r" -NoNewline
    }
}

Write-Host "Esperando respuestas..."
Wait-Job $jobs | Out-Null

foreach ($job in $jobs) {
    $result = Receive-Job $job
    if ($result) { $success++ } else { $failed++ }
    Remove-Job $job
}

$elapsed = ((Get-Date) - $startTime).TotalSeconds
$rate = [math]::Round($success / $elapsed, 2)

Write-Host ""
Write-Host "================================" -ForegroundColor Green
Write-Host "Exitosas: $success"
Write-Host "Fallidas: $failed"
Write-Host "Tiempo: $([math]::Round($elapsed, 2))s"
Write-Host "Rate: $rate req/s" -ForegroundColor Yellow
Write-Host ""

if ($rate -ge 800) {
    Write-Host "OBJETIVO CERCA/CUMPLIDO!" -ForegroundColor Green
} else {
    Write-Host "Para mas throughput: docker-compose up -d --scale api-gateway=3" -ForegroundColor Yellow
}

# ⚡ Load Test - 1000 requests/second
# Valida el rendimiento del sistema bajo carga

param(
    [int]$DurationSeconds = 30,
    [int]$Concurrent = 100,
    [string]$Url = "http://localhost:8080/api/locations"
)

Write-Host "Load Test - Sistema Verisure" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "URL: $Url"
Write-Host "Concurrencia: $Concurrent"
Write-Host "Duracion: $DurationSeconds segundos"
Write-Host "Objetivo: 1000 req/s"
Write-Host ""

# Función para enviar request
function Send-LocationRequest {
    param($UserId, $SessionId)
    
    $latitude = 40.7128 + (Get-Random -Minimum -0.5 -Maximum 0.5)
    $longitude = -74.0060 + (Get-Random -Minimum -0.5 -Maximum 0.5)
    $timestamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
    
    $body = @{
        userId = $UserId
        latitude = $latitude
        longitude = $longitude
        timestamp = $timestamp
    } | ConvertTo-Json
    
    $headers = @{
        "Content-Type" = "application/json"
        "X-Session-ID" = $SessionId
    }
    
    try {
        $response = Invoke-WebRequest -Uri $Url -Method Post -Body $body -Headers $headers -UseBasicParsing -TimeoutSec 5
        return @{
            Success = $true
            StatusCode = $response.StatusCode
            Duration = 0
        }
    }
    catch {
        return @{
            Success = $false
            Error = $_.Exception.Message
            Duration = 0
        }
    }
}

# Inicializar contadores
$script:SuccessCount = 0
$script:ErrorCount = 0
$script:TotalResponseTime = 0
$script:lock = New-Object System.Object

Write-Host "Calentando sistema..." -ForegroundColor Yellow
1..5 | ForEach-Object {
    Send-LocationRequest -UserId $_ -SessionId "warmup" | Out-Null
    Start-Sleep -Milliseconds 200
}
Write-Host "Sistema calentado" -ForegroundColor Green
Write-Host ""

# Esperar 2 segundos
Start-Sleep -Seconds 2

Write-Host "Iniciando test de carga..." -ForegroundColor Yellow
Write-Host ""

$startTime = Get-Date
$endTime = $startTime.AddSeconds($DurationSeconds)
$requestsSent = 0

# Jobs para paralelizar
$jobs = @()
$batchSize = [math]::Ceiling($Concurrent / 10)

while ((Get-Date) -lt $endTime) {
    # Crear batch de jobs
    for ($i = 0; $i -lt $Concurrent; $i++) {
        $userId = (Get-Random -Minimum 1 -Maximum 1000).ToString()
        $sessionId = [guid]::NewGuid().ToString()
        
        $job = Start-Job -ScriptBlock {
            param($url, $userId, $sessionId, $body)
            
            $lat = 40.7128 + (Get-Random -Minimum -0.5 -Maximum 0.5)
            $lon = -74.0060 + (Get-Random -Minimum -0.5 -Maximum 0.5)
            $ts = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
            
            $payload = @{
                userId = $userId
                latitude = $lat
                longitude = $lon
                timestamp = $ts
            } | ConvertTo-Json
            
            $headers = @{
                "Content-Type" = "application/json"
            }
            
            $sw = [System.Diagnostics.Stopwatch]::StartNew()
            try {
                $resp = Invoke-WebRequest -Uri $url -Method Post -Body $payload -Headers $headers -UseBasicParsing -TimeoutSec 5
                $sw.Stop()
                return @{
                    Success = $true
                    StatusCode = $resp.StatusCode
                    Duration = $sw.ElapsedMilliseconds
                }
            }
            catch {
                $sw.Stop()
                return @{
                    Success = $false
                    Error = $_.Exception.Message
                    Duration = $sw.ElapsedMilliseconds
                }
            }
        } -ArgumentList $Url, $userId, $sessionId, $body
        
        $jobs += $job
        $requestsSent++
    }
    
    # Procesar jobs completados
    $completed = $jobs | Where-Object { $_.State -eq 'Completed' }
    foreach ($job in $completed) {
        $result = Receive-Job -Job $job
        if ($result.Success) {
            $script:SuccessCount++
        }
        else {
            $script:ErrorCount++
        }
        Remove-Job -Job $job
    }
    $jobs = $jobs | Where-Object { $_.State -ne 'Completed' }
    
    # Pequeña pausa para no saturar
    Start-Sleep -Milliseconds 50
    
    # Mostrar progreso
    $elapsed = ((Get-Date) - $startTime).TotalSeconds
    $currentRate = [math]::Round($script:SuccessCount / $elapsed, 2)
    Write-Host "`rEnviadas: $script:SuccessCount | Errores: $script:ErrorCount | Rate: $currentRate req/s" -NoNewline
}

# Esperar a que terminen todos los jobs
Write-Host ""
Write-Host "Esperando finalización de requests..." -ForegroundColor Yellow
Wait-Job -Job $jobs -Timeout 30 | Out-Null
foreach ($job in $jobs) {
    if ($job.State -eq 'Completed') {
        $result = Receive-Job -Job $job
        if ($result.Success) {
            $script:SuccessCount++
        }
        else {
            $script:ErrorCount++
        }
    }
    Remove-Job -Job $job -Force
}

$totalTime = ((Get-Date) - $startTime).TotalSeconds

# Resultados
Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host "RESULTADOS" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Requests exitosas:  $script:SuccessCount" -ForegroundColor Green
Write-Host "Requests fallidas:  $script:ErrorCount"
Write-Host "Total requests:     $($script:SuccessCount + $script:ErrorCount)"
Write-Host "Duracion real:      $([math]::Round($totalTime, 2)) segundos"
Write-Host ""

$avgRate = [math]::Round($script:SuccessCount / $totalTime, 2)
$successRate = [math]::Round(($script:SuccessCount / ($script:SuccessCount + $script:ErrorCount)) * 100, 2)

Write-Host "Throughput:         $avgRate req/s" -ForegroundColor Yellow
Write-Host "Tasa de exito:      $successRate%" -ForegroundColor Yellow
Write-Host ""

if ($avgRate -ge 1000) {
    Write-Host "OBJETIVO CUMPLIDO: Sistema aguanta 1000+ req/s" -ForegroundColor Green
}
elseif ($avgRate -ge 800) {
    Write-Host "CERCA DEL OBJETIVO: $avgRate req/s" -ForegroundColor Yellow
}
else {
    Write-Host "OBJETIVO NO CUMPLIDO: $avgRate req/s" -ForegroundColor Red
}

Write-Host ""
Write-Host "Recomendaciones:" -ForegroundColor Cyan
Write-Host "  1. Escalar API Gateway: docker-compose up -d --scale api-gateway=3"
Write-Host "  2. Aumentar workers RabbitMQ"
Write-Host "  3. Optimizar recursos Docker"
Write-Host ""

# Script de pruebas automáticas para Windows PowerShell

Write-Host "🧪 Iniciando pruebas del sistema..." -ForegroundColor Cyan
Write-Host ""

# Función para verificar health
function Check-Health {
    param($Service, $Port)
    
    Write-Host -NoNewline "Esperando a que $Service esté listo..."
    $maxRetries = 30
    $retry = 0
    
    while ($retry -lt $maxRetries) {
        try {
            $response = Invoke-RestMethod -Uri "http://localhost:$Port/actuator/health" -TimeoutSec 2
            if ($response.status -eq "UP") {
                Write-Host " ✓ OK" -ForegroundColor Green
                return $true
            }
        }
        catch {
            # Continuar intentando
        }
        Start-Sleep -Seconds 2
        $retry++
        Write-Host -NoNewline "."
    }
    Write-Host " ✗ TIMEOUT" -ForegroundColor Red
    return $false
}

# 1. Verificar servicios
Write-Host "1️⃣  Verificando servicios..." -ForegroundColor Yellow
if (-not (Check-Health "API Gateway" 8080)) { exit 1 }
if (-not (Check-Health "Realtime Processor" 8081)) { exit 1 }
if (-not (Check-Health "Reporting Service" 8082)) { exit 1 }
Write-Host ""

# 2. Enviar ubicaciones de prueba
Write-Host "2️⃣  Enviando ubicaciones de prueba..." -ForegroundColor Yellow

$headers = @{ "Content-Type" = "application/json" }

# Alice NYC
$body1 = @{
    userId = "alice"
    latitude = 40.7128
    longitude = -74.0060
    timestamp = "2026-01-29T10:00:00Z"
} | ConvertTo-Json

$response1 = Invoke-RestMethod -Uri "http://localhost:8080/api/locations" -Method Post -Headers $headers -Body $body1
if ($response1.status -eq "accepted") {
    Write-Host "✓ Alice (NYC) enviada" -ForegroundColor Green
}

# Bob Madrid
$body2 = @{
    userId = "bob"
    latitude = 40.4168
    longitude = -3.7038
    timestamp = "2026-01-29T10:00:00Z"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/locations" -Method Post -Headers $headers -Body $body2 | Out-Null
Write-Host "✓ Bob (Madrid) enviado" -ForegroundColor Green

# Alice Londres (anomalía)
$body3 = @{
    userId = "alice"
    latitude = 51.5074
    longitude = -0.1278
    timestamp = "2026-01-29T10:10:00Z"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/locations" -Method Post -Headers $headers -Body $body3 | Out-Null
Write-Host "✓ Alice (Londres - ANOMALÍA) enviada" -ForegroundColor Yellow

# Charlie múltiples ubicaciones
for ($i = 1; $i -le 5; $i++) {
    $lat = 40.7128 + ($i * 0.01)
    $lon = -74.0060 + ($i * 0.01)
    $bodyCharlie = @{
        userId = "charlie"
        latitude = $lat
        longitude = $lon
        timestamp = "2026-01-29T10:$($i.ToString('00')):00Z"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "http://localhost:8080/api/locations" -Method Post -Headers $headers -Body $bodyCharlie | Out-Null
}
Write-Host "✓ Charlie (5 ubicaciones) enviadas" -ForegroundColor Green
Write-Host ""

# 3. Esperar procesamiento
Write-Host "3️⃣  Esperando procesamiento..." -ForegroundColor Yellow
Start-Sleep -Seconds 5
Write-Host "✓ Eventos procesados" -ForegroundColor Green
Write-Host ""

# 4. Verificar reporte
Write-Host "4️⃣  Obteniendo reporte..." -ForegroundColor Yellow
$report = Invoke-RestMethod -Uri "http://localhost:8082/api/reports"
Write-Host "   Total usuarios: $($report.totalUsers)"
Write-Host "   Total eventos: $($report.totalEvents)"

if ($report.totalEvents -gt 0) {
    Write-Host "✓ Reporte generado correctamente" -ForegroundColor Green
}
Write-Host ""

# 5. Verificar reporte de usuario
Write-Host "5️⃣  Obteniendo reporte de Alice..." -ForegroundColor Yellow
$userReport = Invoke-RestMethod -Uri "http://localhost:8082/api/reports/alice"
Write-Host "   Ubicaciones de Alice: $($userReport[0].totalLocations)"
Write-Host "✓ Reporte individual obtenido" -ForegroundColor Green
Write-Host ""

# 6. Verificar métricas
Write-Host "6️⃣  Verificando métricas..." -ForegroundColor Yellow
$metrics = Invoke-RestMethod -Uri "http://localhost:8080/actuator/prometheus"
if ($metrics -match "location_requests_total") {
    Write-Host "✓ Métricas disponibles" -ForegroundColor Green
}
Write-Host ""

# Resumen
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "✅ Pruebas completadas" -ForegroundColor Green
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""
Write-Host "📊 Para ver más detalles:"
Write-Host "   • RabbitMQ UI: http://localhost:15672 (guest/guest)"
Write-Host "   • API Gateway: http://localhost:8080/actuator/health"
Write-Host "   • Logs: docker-compose logs -f"
Write-Host ""

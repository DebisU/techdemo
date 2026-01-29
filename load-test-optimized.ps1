# Load Test Optimizado - Techdemo (Microservicios)
# Usa .NET HttpClient para mejor rendimiento

param(
    [int]$DurationSeconds = 30,
    [int]$Concurrent = 200,
    [string]$Url = "http://localhost:8080/api/locations",
    [int]$TargetRps = 1000  # Target: 1000 requests per SECOND
)

# Agregar ensamblados necesarios
Add-Type -AssemblyName System.Net.Http

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   Load Test - Techdemo (Microservicios)"  -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "URL:          $Url"
Write-Host "Concurrencia: $Concurrent conexiones simultaneas"
Write-Host "Duracion:     $DurationSeconds segundos"
Write-Host "Objetivo:     $TargetRps req/s (NFR requirement)"
Write-Host ""

# Verificar disponibilidad
Write-Host "Verificando disponibilidad del servicio..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get -TimeoutSec 5
    if ($health.status -eq "UP") {
        Write-Host "Servicio disponible" -ForegroundColor Green
    } else {
        Write-Host "Servicio no disponible: $($health.status)" -ForegroundColor Red
        exit 1
    }
}
catch {
    Write-Host "Error conectando al servicio: $_" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Calentamiento
Write-Host "Calentando sistema (5 requests)..." -ForegroundColor Yellow
$userPool = 1..100 | ForEach-Object { "user-$($_.ToString('D3'))" }
1..5 | ForEach-Object {
    $payload = @{
        userId = $userPool | Get-Random
        latitude = 40.4168 + (Get-Random -Minimum -50 -Maximum 50) / 1000
        longitude = -3.7038 + (Get-Random -Minimum -50 -Maximum 50) / 1000
        timestamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
    } | ConvertTo-Json
    Invoke-RestMethod -Uri $Url -Method Post -Body $payload -ContentType "application/json" -TimeoutSec 5 | Out-Null
    Start-Sleep -Milliseconds 100
}
Write-Host "Sistema calentado" -ForegroundColor Green
Write-Host ""

# Estado inicial (si hay endpoint de stats)
try {
    $queueStats = Invoke-RestMethod -Uri "http://localhost:8080/api/locations/stats" -Method Get -ErrorAction SilentlyContinue
    Write-Host "Estado inicial del sistema:"
    Write-Host "  Eventos procesados: $($queueStats.totalProcessed)"
    Write-Host ""
} catch {
    Write-Host "Estado inicial del sistema: No disponible" -ForegroundColor Yellow
    Write-Host ""
}

# Contadores
$script:SuccessCount = 0
$script:ErrorCount = 0
$script:TotalDuration = 0

Write-Host "Iniciando test de carga..." -ForegroundColor Yellow
Write-Host ""

$startTime = Get-Date
$endTime = $startTime.AddSeconds($DurationSeconds)

# Crear HttpClient reutilizable
$httpClient = New-Object System.Net.Http.HttpClient
$httpClient.Timeout = [TimeSpan]::FromSeconds(10)

# Pool de tareas
$tasks = [System.Collections.Generic.List[System.Threading.Tasks.Task]]::new()

while ((Get-Date) -lt $endTime) {
    # Limpiar tareas completadas
    $completed = @()
    foreach ($task in $tasks) {
        if ($task.IsCompleted) {
            $completed += $task
            if ($task.Status -eq 'RanToCompletion') {
                $script:SuccessCount++
            } else {
                $script:ErrorCount++
            }
        }
    }
    foreach ($task in $completed) {
        $tasks.Remove($task) | Out-Null
        $task.Dispose()
    }
    
    # Crear nuevas tareas hasta la concurrencia
    while ($tasks.Count -lt $Concurrent -and (Get-Date) -lt $endTime) {
        $userId = $userPool | Get-Random
        $lat = 40.4168 + (Get-Random -Minimum -50 -Maximum 50) / 1000
        $lon = -3.7038 + (Get-Random -Minimum -50 -Maximum 50) / 1000
        $ts = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
        
        $payload = @{
            userId = $userId
            latitude = $lat
            longitude = $lon
            timestamp = $ts
        } | ConvertTo-Json
        
        $content = New-Object System.Net.Http.StringContent($payload, [System.Text.Encoding]::UTF8, "application/json")
        
        $task = $httpClient.PostAsync($Url, $content)
        $tasks.Add($task)
    }
    
    # Mostrar progreso cada segundo
    $elapsed = ((Get-Date) - $startTime).TotalSeconds
    if ($elapsed -ge 1 -and ($elapsed % 1) -lt 0.1) {
        $currentRate = [math]::Round($script:SuccessCount / $elapsed, 1)
        $progressMsg = "`rOK: $script:SuccessCount | ERR: $script:ErrorCount | $currentRate req/s   "
        Write-Host $progressMsg -NoNewline -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host ""
Write-Host "Esperando tareas finales..." -ForegroundColor Yellow

# Esperar tareas pendientes con timeout
$timeout = (Get-Date).AddSeconds(10)
while ($tasks.Count -gt 0 -and (Get-Date) -lt $timeout) {
    $completed = @()
    foreach ($task in $tasks) {
        if ($task.IsCompleted) {
            $completed += $task
            if ($task.Status -eq 'RanToCompletion') {
                $script:SuccessCount++
            } else {
                $script:ErrorCount++
            }
        }
    }
    foreach ($task in $completed) {
        $tasks.Remove($task) | Out-Null
        $task.Dispose()
    }
    Start-Sleep -Milliseconds 100
}

# Limpiar tareas restantes
foreach ($task in $tasks) {
    $task.Dispose()
}
$httpClient.Dispose()

$totalTime = ((Get-Date) - $startTime).TotalSeconds

# Estado final
Start-Sleep -Seconds 2
try {
    $queueStatsFinal = Invoke-RestMethod -Uri "http://localhost:8080/api/locations/stats" -Method Get -ErrorAction SilentlyContinue
} catch {
    $queueStatsFinal = $null
}

# Resultados
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "            RESULTADOS                  " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$totalRequests = $script:SuccessCount + $script:ErrorCount
$throughput = [math]::Round($script:SuccessCount / $totalTime, 2)
$rpm = [math]::Round($throughput * 60, 0)
$successRate = if ($totalRequests -gt 0) { [math]::Round(($script:SuccessCount / $totalRequests) * 100, 2) } else { 0 }

Write-Host "Requests exitosas:  $script:SuccessCount"
Write-Host "Requests fallidas:  $script:ErrorCount"
Write-Host "Total requests:     $totalRequests"
Write-Host "Duracion real:      $([math]::Round($totalTime, 2)) segundos"
Write-Host ""
Write-Host "Throughput:         $throughput req/s ($rpm req/min)"
Write-Host "Tasa de exito:      $successRate%"
Write-Host ""

if ($queueStatsFinal) {
    Write-Host "Estado del sistema:"
    Write-Host "  Eventos procesados: $($queueStatsFinal.totalProcessed)"
    Write-Host ""
}

# Evaluacion
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "            EVALUACION                  " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($throughput -ge $TargetRps) {
    Write-Host "[OK] OBJETIVO CUMPLIDO: $throughput req/s >= $TargetRps req/s" -ForegroundColor Green
} elseif ($throughput -ge ($TargetRps * 0.8)) {
    Write-Host "[~] OBJETIVO CASI CUMPLIDO: $throughput req/s (80%+ del target $TargetRps req/s)" -ForegroundColor Yellow
} else {
    Write-Host "[X] OBJETIVO NO CUMPLIDO: $throughput req/s < $TargetRps req/s" -ForegroundColor Red
}

if ($successRate -ge 99) {
    Write-Host "[OK] TASA DE EXITO: $successRate% >= 99%" -ForegroundColor Green
} else {
    Write-Host "[X] TASA DE EXITO BAJA: $successRate% menor a 99%" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan

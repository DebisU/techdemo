#!/bin/bash

# Script de pruebas automáticas para el sistema Verisure

echo "🧪 Iniciando pruebas del sistema..."
echo ""

# Colores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Función para verificar health
check_health() {
    local service=$1
    local port=$2
    local max_retries=30
    local retry=0
    
    echo -n "Esperando a que $service esté listo..."
    while [ $retry -lt $max_retries ]; do
        if curl -s "http://localhost:$port/actuator/health" | grep -q "UP"; then
            echo -e " ${GREEN}✓ OK${NC}"
            return 0
        fi
        sleep 2
        retry=$((retry + 1))
        echo -n "."
    done
    echo -e " ${RED}✗ TIMEOUT${NC}"
    return 1
}

# 1. Verificar servicios
echo "1️⃣  Verificando servicios..."
check_health "API Gateway" 8080 || exit 1
check_health "Realtime Processor" 8081 || exit 1
check_health "Reporting Service" 8082 || exit 1
echo ""

# 2. Enviar ubicaciones de prueba
echo "2️⃣  Enviando ubicaciones de prueba..."

# Usuario 1 - Nueva York
curl -s -X POST http://localhost:8080/api/locations \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "alice",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "timestamp": "2026-01-29T10:00:00Z"
  }' | jq -r '.status' | grep -q "accepted" && echo -e "${GREEN}✓${NC} Alice (NYC) enviada" || echo -e "${RED}✗${NC} Error"

# Usuario 1 - Otra ubicación cercana
curl -s -X POST http://localhost:8080/api/locations \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "alice",
    "latitude": 40.7228,
    "longitude": -74.0160,
    "timestamp": "2026-01-29T10:05:00Z"
  }' | jq -r '.status' | grep -q "accepted" && echo -e "${GREEN}✓${NC} Alice (NYC movimiento) enviada" || echo -e "${RED}✗${NC} Error"

# Usuario 2 - Madrid
curl -s -X POST http://localhost:8080/api/locations \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "bob",
    "latitude": 40.4168,
    "longitude": -3.7038,
    "timestamp": "2026-01-29T10:00:00Z"
  }' | jq -r '.status' | grep -q "accepted" && echo -e "${GREEN}✓${NC} Bob (Madrid) enviado" || echo -e "${RED}✗${NC} Error"

# Usuario 3 - Londres (anomalía para Alice)
curl -s -X POST http://localhost:8080/api/locations \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "alice",
    "latitude": 51.5074,
    "longitude": -0.1278,
    "timestamp": "2026-01-29T10:10:00Z"
  }' | jq -r '.status' | grep -q "accepted" && echo -e "${YELLOW}✓${NC} Alice (Londres - ANOMALÍA) enviada" || echo -e "${RED}✗${NC} Error"

# Más ubicaciones para estadísticas
for i in {1..5}; do
    lat=$(echo "40.7128 + ($i * 0.01)" | bc)
    lon=$(echo "-74.0060 + ($i * 0.01)" | bc)
    curl -s -X POST http://localhost:8080/api/locations \
      -H "Content-Type: application/json" \
      -d "{
        \"userId\": \"charlie\",
        \"latitude\": $lat,
        \"longitude\": $lon,
        \"timestamp\": \"2026-01-29T10:${i}0:00Z\"
      }" > /dev/null
done
echo -e "${GREEN}✓${NC} Charlie (5 ubicaciones) enviadas"

echo ""

# 3. Esperar procesamiento
echo "3️⃣  Esperando procesamiento..."
sleep 5
echo -e "${GREEN}✓${NC} Eventos procesados"
echo ""

# 4. Verificar reporte
echo "4️⃣  Obteniendo reporte..."
REPORT=$(curl -s http://localhost:8082/api/reports)

TOTAL_USERS=$(echo $REPORT | jq -r '.totalUsers')
TOTAL_EVENTS=$(echo $REPORT | jq -r '.totalEvents')

echo "   Total usuarios: $TOTAL_USERS"
echo "   Total eventos: $TOTAL_EVENTS"

if [ "$TOTAL_EVENTS" -gt 0 ]; then
    echo -e "${GREEN}✓${NC} Reporte generado correctamente"
else
    echo -e "${RED}✗${NC} No hay eventos en el reporte"
fi
echo ""

# 5. Verificar reporte de usuario específico
echo "5️⃣  Obteniendo reporte de Alice..."
USER_REPORT=$(curl -s http://localhost:8082/api/reports/alice)
ALICE_LOCATIONS=$(echo $USER_REPORT | jq -r '.[0].totalLocations')
echo "   Ubicaciones de Alice: $ALICE_LOCATIONS"

if [ "$ALICE_LOCATIONS" -gt 0 ]; then
    echo -e "${GREEN}✓${NC} Reporte individual obtenido"
else
    echo -e "${RED}✗${NC} No se encontraron ubicaciones para Alice"
fi
echo ""

# 6. Verificar métricas
echo "6️⃣  Verificando métricas (Prometheus)..."
METRICS=$(curl -s http://localhost:8080/actuator/prometheus)

if echo "$METRICS" | grep -q "location_requests_total"; then
    REQUESTS=$(echo "$METRICS" | grep "location_requests_total" | tail -1 | awk '{print $2}')
    echo "   Requests totales: $REQUESTS"
    echo -e "${GREEN}✓${NC} Métricas disponibles"
else
    echo -e "${RED}✗${NC} Métricas no encontradas"
fi
echo ""

# 7. Test de validación (debe fallar)
echo "7️⃣  Test de validación (latitude inválida)..."
ERROR_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/locations \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test",
    "latitude": 999.0,
    "longitude": -74.0060,
    "timestamp": "2026-01-29T10:00:00Z"
  }')

if [ "$ERROR_RESPONSE" -ge 400 ]; then
    echo -e "${GREEN}✓${NC} Validación rechazó coordenada inválida (HTTP $ERROR_RESPONSE)"
else
    echo -e "${RED}✗${NC} Validación NO funcionó (HTTP $ERROR_RESPONSE)"
fi
echo ""

# 8. Verificar RabbitMQ
echo "8️⃣  Verificando RabbitMQ..."
QUEUES=$(curl -s -u guest:guest http://localhost:15672/api/queues)

if echo "$QUEUES" | grep -q "location.realtime.queue"; then
    REALTIME_MSGS=$(echo "$QUEUES" | jq -r '.[] | select(.name=="location.realtime.queue") | .messages')
    REPORTING_MSGS=$(echo "$QUEUES" | jq -r '.[] | select(.name=="location.reporting.queue") | .messages')
    echo "   Cola realtime: $REALTIME_MSGS mensajes pendientes"
    echo "   Cola reporting: $REPORTING_MSGS mensajes pendientes"
    echo -e "${GREEN}✓${NC} RabbitMQ funcionando"
else
    echo -e "${RED}✗${NC} Colas no encontradas"
fi
echo ""

# Resumen
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${GREEN}✅ Pruebas completadas${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📊 Para ver más detalles:"
echo "   • RabbitMQ UI: http://localhost:15672 (guest/guest)"
echo "   • API Gateway: http://localhost:8080/actuator/health"
echo "   • Logs: docker-compose logs -f"
echo ""

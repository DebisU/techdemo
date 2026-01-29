# Verisure TechTest - Sistema de Procesamiento de Ubicaciones en Alta Disponibilidad

> **Prueba Técnica** - Diseño e implementación de infraestructura de alta disponibilidad para procesamiento de ubicaciones  
> **Objetivo**: ~1000 req/s | 99.99% SLA | Tráfico variable | Real-time + Reporting

![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-purple?logo=kotlin)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.12-green?logo=springboot)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.13-orange?logo=rabbitmq)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-7-red?logo=redis)

---

## 📊 Diagrama de Arquitectura

```
                                   ┌─────────────────────────┐
                                   │       CLIENTS           │
                                   │   (Mobile/IoT/Web)      │
                                   └───────────┬─────────────┘
                                               │ HTTP POST
                                               ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                            🌐 API GATEWAY (:8080)                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐     │
│  │  POST /api/locations                                                    │     │
│  │  • Bean Validation (userId, lat/lon ranges)                             │     │
│  │  • LocationRequest → LocationEvent transformation                       │     │
│  │  • Returns 202 Accepted (async processing)                              │     │
│  │  • Metrics: location_gateway_events_total, timing                       │     │
│  └────────────────────────────────────┬────────────────────────────────────┘     │
└───────────────────────────────────────┼──────────────────────────────────────────┘
                                        │ Publish (JSON)
                                        ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                         🐰 RABBITMQ (Message Broker)                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐     │
│  │           Exchange: location.exchange (FANOUT)                          │     │
│  │                           │                                             │     │
│  │              ┌────────────┴────────────┐                                │     │
│  │              ▼                         ▼                                │     │
│  │    ┌─────────────────┐      ┌─────────────────────┐                    │     │
│  │    │ location.       │      │ location.           │                    │     │
│  │    │ realtime.queue  │      │ reporting.queue     │                    │     │
│  │    │ (durable)       │      │ (durable)           │                    │     │
│  │    └────────┬────────┘      └──────────┬──────────┘                    │     │
│  │             │                          │                               │     │
│  └─────────────┼──────────────────────────┼───────────────────────────────┘     │
└────────────────┼──────────────────────────┼─────────────────────────────────────┘
                 │ Consume                  │ Consume
                 ▼                          ▼
┌────────────────────────────────┐   ┌─────────────────────────────────────┐
│ ⚡ REALTIME PROCESSOR (:8081)  │   │ 📊 REPORTING SERVICE (:8082)        │
│ ┌────────────────────────────┐ │   │ ┌─────────────────────────────────┐ │
│ │ • 3-10 concurrent workers  │ │   │ │ • 2-5 concurrent workers        │ │
│ │ • Prefetch: 50 messages    │ │   │ │ • Prefetch: 100 messages        │ │
│ │ • Anomaly detection        │ │   │ │ • Batch persistence             │ │
│ │ • Distance calculation     │ │   │ │ • Report generation             │ │
│ │ • Haversine formula        │ │   │ │ • Cache invalidation            │ │
│ └─────────────┬──────────────┘ │   │ └────────┬──────────┬─────────────┘ │
│               │                │   │          │          │               │
│               ▼                │   │          ▼          ▼               │
│   ┌───────────────────┐        │   │ ┌──────────┐  ┌──────────┐         │
│   │   🔴 REDIS (:6379) │        │   │ │ 🐘 PostgreSQL │  │🔴 REDIS │   │
│   │   • Last location │        │   │ │  (:5432)      │  │  Cache  │   │
│   │   • Anomaly count │        │   │ │ location_events│  │ reports │   │
│   │   • TTL: 1 hour   │        │   │ │ (indexed)     │  │ TTL:60s │   │
│   └───────────────────┘        │   │ └──────────────┘  └─────────┘    │
└────────────────────────────────┘   └─────────────────────────────────────┘

                              📈 OBSERVABILITY STACK
┌──────────────────────────────────────────────────────────────────────────────────┐
│  ┌───────────────┐    ┌───────────────┐    ┌───────────────┐    ┌────────────┐  │
│  │  Prometheus   │───▶│    Grafana    │    │     Loki      │◀───│  Promtail  │  │
│  │   (:9090)     │    │   (:3000)     │◀───│   (:3100)     │    │            │  │
│  │ metrics scrape│    │  dashboards   │    │  log storage  │    │ log collect│  │
│  └───────────────┘    └───────────────┘    └───────────────┘    └────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🚀 Quick Start

### Prerrequisitos
- Java 17+ (probado con Java 17, 21 y 25)
- Docker & Docker Compose
- Gradle 8.x (incluido wrapper)

### Iniciar Todo el Sistema
```powershell
# 1. Construir todos los módulos
./gradlew build

# 2. Levantar infraestructura + microservicios
docker-compose up -d

# 3. Verificar estado
docker-compose ps
```

### URLs del Sistema

| Servicio | URL | Credenciales |
|----------|-----|--------------|
| API Gateway | http://localhost:8080 | - |
| Realtime Processor | http://localhost:8081 | - |
| Reporting Service | http://localhost:8082 | - |
| RabbitMQ Management | http://localhost:15672 | guest/guest |
| **Grafana Dashboard** | http://localhost:3000 | admin/admin |
| Prometheus | http://localhost:9090 | - |

---

## 📦 Estructura del Proyecto

```
techdemo/
├── shared-models/          # DTOs compartidos (LocationEvent, LocationRequest, etc.)
├── api-gateway/            # Microservicio A - REST API + Publisher RabbitMQ
├── realtime-processor/     # Microservicio B - Procesamiento en tiempo real
├── reporting-service/      # Microservicio C - Persistencia + Reportes
├── observability/          # Configuración Prometheus, Grafana, Loki
├── k6/                     # Scripts de load testing
├── docker-compose.yml      # Infraestructura completa
├── ARCHITECTURE.md         # Diseño arquitectónico detallado
└── docs/                   # Documentación adicional
```

---

## 🧪 Testing

### Ejecutar Tests Unitarios
```powershell
# Todos los tests
./gradlew test

# Tests específicos por módulo
./gradlew :api-gateway:test
./gradlew :realtime-processor:test
./gradlew :reporting-service:test
```

### Test Manual de Ubicación
```bash
curl -X POST http://localhost:8080/api/locations \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-001",
    "latitude": 40.4168,
    "longitude": -3.7038,
    "timestamp": "2024-01-15T10:30:00Z"
  }'
```

**Respuesta esperada (202 Accepted):**
```json
{
  "status": "accepted",
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Location event queued for processing"
}
```

### Consultar Reportes
```bash
# Reporte global (cached)
curl http://localhost:8082/api/reports

# Reporte de usuario específico
curl http://localhost:8082/api/reports/user-001
```

---

## 📊 Load Testing

El sistema ha sido probado para superar el requisito de **1000 req/s**:

```powershell
# Ejecutar load test con k6
docker run --rm -i --network techdemo_default grafana/k6 run - < k6/load-test.js
```

### Resultados Obtenidos

| Métrica | Resultado | Objetivo |
|---------|-----------|----------|
| **Throughput** | 2444 req/s | 1000 req/s ✅ |
| **Latencia p50** | 12ms | - |
| **Latencia p95** | 45ms | - |
| **Tasa de éxito** | 99.98% | 99.99% ✅ |

---

## 📈 Observabilidad

### Grafana Dashboard
Accede a http://localhost:3000 (admin/admin) para ver el dashboard **"Verisure Location System Overview"** con:

- **HTTP Traffic**: Requests/sec, latencia, errores
- **RabbitMQ**: Mensajes publicados, tasas de consumo
- **Sistema**: CPU, memoria por servicio
- **PostgreSQL**: Eventos almacenados
- **Redis**: Operaciones de cache

### Métricas Prometheus
```bash
# Métricas del API Gateway
curl http://localhost:8080/actuator/prometheus | grep location

# Métricas principales:
# - location_gateway_events_total
# - location_realtime_processed_total
# - location_reporting_saved_total
```

---

## 🏗️ Decisiones Arquitectónicas

### ¿Por qué RabbitMQ en lugar de Kafka?
- **1000 req/s NO necesita Kafka** (overkill para este volumen)
- RabbitMQ es más simple y tiene menos overhead operacional
- **Fanout pattern** es nativo y eficiente
- Para millones de eventos/seg → entonces Kafka

### ¿Por qué 202 Accepted en lugar de 200 OK?
- El procesamiento es **asíncrono**
- El cliente no debe esperar por la persistencia ni el análisis
- Semántica HTTP correcta para operaciones en cola

### ¿Por qué separar Realtime y Reporting?
- **Single Responsibility**: Cada servicio hace una cosa bien
- **Escalado independiente**: Realtime necesita más workers que Reporting
- **Tolerancia a fallos**: Si Reporting cae, Realtime sigue funcionando

---

## 📖 Documentación del Código

El código está completamente documentado con comentarios KDoc (`/** ... */`).

### Generar Documentación HTML

```powershell
# Generar documentación HTML para todos los módulos
./gradlew dokkaHtmlMultiModule

# Abrir en navegador
Start-Process build\dokka\htmlMultiModule\index.html
```

**Salida**: `build/dokka/htmlMultiModule/index.html`

### Archivos Documentados (18 archivos - 100%)

| Módulo | Archivos | Clases Principales |
|--------|----------|-------------------|
| **api-gateway** | 4 | ApiGatewayApplication, LocationController, EventPublisher, RabbitMQConfig |
| **realtime-processor** | 4 | RealtimeAnalyticsService (fórmula Haversine), LocationEventConsumer |
| **reporting-service** | 7 | PersistenceService, ReportService, ReportController, LocationEntity |
| **shared-models** | 3 | LocationEvent, LocationRequest, LocationReport |

### Contenido de la Documentación

- Descripción de clases con rol arquitectónico
- Descripción de métodos con @param, @return, @throws
- Diagramas ASCII de flujos de datos
- Ejemplos de JSON request/response
- Constantes documentadas (umbrales, fórmulas matemáticas)

---

## 📝 Notas para Producción

Para un despliegue en producción, considerar:

1. **Kubernetes**: Usar Helm charts para orquestación
2. **Secrets Management**: Vault o AWS Secrets Manager
3. **Database Migrations**: Flyway o Liquibase
4. **Circuit Breakers**: Resilience4j ya configurado
5. **Rate Limiting**: Spring Cloud Gateway
6. **TLS/SSL**: Nginx como reverse proxy
7. **Horizontal Scaling**: HPA en Kubernetes

---

## �️ Comandos Útiles

### Desarrollo Local (Sin Docker)
```powershell
# Iniciar solo infraestructura
docker-compose up rabbitmq postgres redis -d

# Ejecutar microservicios localmente
./gradlew :api-gateway:bootRun
./gradlew :realtime-processor:bootRun
./gradlew :reporting-service:bootRun
```

### Ver Logs
```powershell
# Logs de todos los servicios
docker-compose logs -f

# Logs específicos
docker-compose logs -f api-gateway
docker-compose logs -f reporting-service
```

### Health Checks
```bash
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8081/actuator/health  # Realtime Processor
curl http://localhost:8082/actuator/health  # Reporting Service
```

### Limpiar Todo
```powershell
# Detener y eliminar contenedores + volúmenes
docker-compose down -v

# Limpiar builds
./gradlew clean
```

---

## 📚 Documentación Adicional

- [ARCHITECTURE.md](ARCHITECTURE.md) - Diseño arquitectónico detallado
- [OBSERVABILITY.md](OBSERVABILITY.md) - Configuración del stack de observabilidad

---

## 👤 Autor

**Verisure Tech Test** - Sistema de demostración para prueba técnica de diseño de infraestructura de alta disponibilidad.

**Tecnologías utilizadas:**
- Spring Boot 4.0.2
- Kotlin 2.2.21
- RabbitMQ 3.13
- PostgreSQL 16
- Redis 7
- Prometheus + Grafana + Loki

---

*Última actualización: Enero 2025*



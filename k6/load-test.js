import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Métricas personalizadas
const errorRate = new Rate('errors');
const locationTrend = new Trend('location_submission_duration');
const successCounter = new Counter('successful_requests');

// Configuración del test
export const options = {
  stages: [
    { duration: '30s', target: 100 },   // Ramp-up a 100 usuarios
    { duration: '1m', target: 500 },    // Escalar a 500 usuarios
    { duration: '2m', target: 1000 },   // Objetivo: 1000 req/s
    { duration: '1m', target: 1000 },   // Mantener carga
    { duration: '30s', target: 0 },     // Ramp-down
  ],
  thresholds: {
    'http_req_duration': ['p(95)<200', 'p(99)<500'], // 95% < 200ms, 99% < 500ms
    'http_req_failed': ['rate<0.01'],                 // Error rate < 1%
    'errors': ['rate<0.05'],                          // Custom error rate < 5%
    'http_reqs': ['rate>800'],                        // Mínimo 800 req/s
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Generar datos realistas
function generateLocation() {
  const userId = Math.floor(Math.random() * 1000) + 1;
  const latitude = 40.7128 + (Math.random() - 0.5) * 0.5;  // NYC ±25km
  const longitude = -74.0060 + (Math.random() - 0.5) * 0.5;
  const timestamp = new Date().toISOString();

  return {
    userId: userId.toString(),
    latitude: latitude,
    longitude: longitude,
    timestamp: timestamp,
  };
}

export default function () {
  const payload = JSON.stringify(generateLocation());
  
  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  // Enviar request
  const res = http.post(`${BASE_URL}/api/locations`, payload, params);

  // Validaciones
  const success = check(res, {
    'status is 202': (r) => r.status === 202,
    'response has eventId': (r) => JSON.parse(r.body).eventId !== undefined,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  // Métricas personalizadas
  errorRate.add(!success);
  locationTrend.add(res.timings.duration);
  if (success) {
    successCounter.add(1);
  }

  // Simular comportamiento realista (no todos los requests son instantáneos)
  sleep(Math.random() * 0.5); // 0-500ms between requests
}

// Setup: validar que el sistema está arriba
export function setup() {
  const res = http.get(`${BASE_URL}/actuator/health`);
  if (res.status !== 200) {
    throw new Error('API Gateway no está disponible');
  }
  console.log('✓ Sistema validado - iniciando load test');
}

// Teardown: generar reporte final
export function teardown(data) {
  console.log('✓ Load test completado - consultar métricas en Grafana');
}

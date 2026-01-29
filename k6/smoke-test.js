import http from 'k6/http';
import { check } from 'k6';

// Test de humo: validar que todo funciona antes del load test completo
export const options = {
  vus: 1, // 1 usuario virtual
  duration: '30s',
  thresholds: {
    'http_req_failed': ['rate<0.01'],
    'http_req_duration': ['p(95)<300'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  // Test 1: Health check
  let res = http.get(`${BASE_URL}/actuator/health`);
  check(res, { 'health check OK': (r) => r.status === 200 });

  // Test 2: Enviar ubicación
  const payload = JSON.stringify({
    userId: '1',
    latitude: 40.7128,
    longitude: -74.0060,
    timestamp: new Date().toISOString(),
  });

  res = http.post(`${BASE_URL}/api/locations`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(res, {
    'location accepted': (r) => r.status === 202,
    'has eventId': (r) => JSON.parse(r.body).eventId !== undefined,
  });
}

export function handleSummary(data) {
  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
  };
}

function textSummary(data, options) {
  const passed = data.metrics.checks.values.passes;
  const failed = data.metrics.checks.values.fails;
  return `
  ✓ Smoke test completado
  ━━━━━━━━━━━━━━━━━━━━━━
  Checks passed: ${passed}
  Checks failed: ${failed}
  Status: ${failed === 0 ? '✓ PASSED' : '✗ FAILED'}
  `;
}

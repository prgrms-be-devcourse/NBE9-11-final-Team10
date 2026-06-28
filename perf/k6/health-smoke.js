import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: Number(__ENV.VUS || 1),
  duration: __ENV.DURATION || '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<300'],
    checks: ['rate>0.99'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const res = http.get(`${baseUrl}/actuator/health`);

  check(res, {
    'health status is 200': (r) => r.status === 200,
    'health is UP': (r) => r.json('status') === 'UP',
  });

  sleep(Number(__ENV.SLEEP_SECONDS || 1));
}

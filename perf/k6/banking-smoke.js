import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

export const options = {
  vus: Number(__ENV.VUS || 1),
  duration: __ENV.DURATION || '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
    checks: ['rate>0.99'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const email = __ENV.TEST_EMAIL;
const password = __ENV.TEST_PASSWORD;
const accountId = __ENV.ACCOUNT_ID;

const loginTrend = new Trend('banking_login_duration');
const journeyFailureRate = new Rate('banking_journey_failed');

function jsonHeaders(token) {
  const headers = {
    'Content-Type': 'application/json;charset=UTF-8',
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return headers;
}

function login() {
  if (!email || !password) {
    throw new Error('TEST_EMAIL and TEST_PASSWORD are required.');
  }

  const startedAt = Date.now();
  const res = http.post(
    `${baseUrl}/api/v1/auth/login`,
    JSON.stringify({ email, password }),
    { headers: jsonHeaders() },
  );
  loginTrend.add(Date.now() - startedAt);

  const ok = check(res, {
    'login status is 200': (r) => r.status === 200,
    'login has accessToken': (r) => Boolean(r.json('accessToken')),
  });

  if (!ok) {
    journeyFailureRate.add(1);
    throw new Error(`Login failed with status ${res.status}: ${res.body}`);
  }

  return res.json('accessToken');
}

export default function () {
  const token = login();
  const authHeaders = { headers: jsonHeaders(token) };

  const me = http.get(`${baseUrl}/api/v1/users/me`, authHeaders);
  const accounts = http.get(`${baseUrl}/api/v1/accounts`, authHeaders);

  const checksPassed = check(me, {
    'me status is 200': (r) => r.status === 200,
  }) && check(accounts, {
    'accounts status is 200': (r) => r.status === 200,
  });

  if (accountId) {
    const transactions = http.get(
      `${baseUrl}/api/v1/accounts/${accountId}/transactions?page=0&sortDirection=DESC`,
      authHeaders,
    );

    check(transactions, {
      'transactions status is 200': (r) => r.status === 200,
    });
  }

  journeyFailureRate.add(checksPassed ? 0 : 1);
  sleep(Number(__ENV.SLEEP_SECONDS || 1));
}

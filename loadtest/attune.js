import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'https://dev.attune-me.com';
const password = __ENV.LOADTEST_PASSWORD || 'AttuneLoadtest!1';
const accountCount = Number(__ENV.ACCOUNT_COUNT || 100);
const soakTest = __ENV.TEST_MODE === 'soak';
const today = new Date().toISOString().slice(0, 10);
const medicationStatus = new Counter('loadtest_medication_status');

export const options = {
  stages: soakTest ? [
    { duration: '30m', target: 25 },
    { duration: '4h', target: 100 },
    { duration: '15m', target: 0 },
  ] : [
    { duration: '2m', target: 10 }, { duration: '10m', target: 10 },
    { duration: '2m', target: 25 }, { duration: '10m', target: 25 },
    { duration: '2m', target: 50 }, { duration: '10m', target: 50 },
    { duration: '2m', target: 75 }, { duration: '10m', target: 75 },
    { duration: '2m', target: 100 }, { duration: '10m', target: 100 },
    { duration: '5m', target: 0 },
  ],
  thresholds: { http_req_failed: ['rate<0.01'], http_req_duration: ['p(95)<1000', 'p(99)<2000'] },
};

export function setup() {
  const accounts = [];
  for (let index = 1; index <= accountCount; index++) {
    const email = `loadtest-${String(index).padStart(3, '0')}@attune.invalid`;
    const response = http.post(`${baseUrl}/v1/auth/login`, JSON.stringify({ email, password }), {
      headers: { 'Content-Type': 'application/json', 'X-Client-Type': 'mobile' },
    });
    check(response, { 'login succeeds': (value) => value.status === 200 });
    accounts.push(JSON.parse(response.body).accessToken);
  }
  return accounts;
}

export default function (tokens) {
  const headers = { Authorization: `Bearer ${tokens[__VU - 1]}` };
  const medications = http.get(`${baseUrl}/v1/user-medications`, { headers });
  medicationStatus.add(1, { status: String(medications.status) });
  check(medications, { 'medications are returned': (value) => value.status === 200 });
  if (__ITER % 5 === 0 && medications.status === 200) {
    const medication = JSON.parse(medications.body)[0];
    http.post(`${baseUrl}/v1/user-medications/${medication.userMedicationId}/log/quick`,
      JSON.stringify({ action: 'TAKEN', scheduleId: medication.schedules[0].scheduleId }),
      { headers: { ...headers, 'Content-Type': 'application/json' } });
  } else {
    http.get(`${baseUrl}/v1/journals/${today}`, { headers });
    http.get(`${baseUrl}/v1/schedules?startDate=${today}&endDate=${today}`, { headers });
  }
  sleep(1 + Math.random() * 2);
}

export function handleSummary(data) {
  delete data.setup_data;
  return { stdout: JSON.stringify(data) };
}

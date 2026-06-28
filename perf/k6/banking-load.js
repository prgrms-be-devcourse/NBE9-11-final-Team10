import { options as smokeOptions } from './banking-smoke.js';
import runJourney from './banking-smoke.js';

export const options = {
  scenarios: {
    warmup: {
      executor: 'constant-vus',
      vus: Number(__ENV.WARMUP_VUS || 2),
      duration: __ENV.WARMUP_DURATION || '30s',
      gracefulStop: '10s',
    },
    sustained_load: {
      executor: 'ramping-vus',
      startTime: __ENV.LOAD_START_TIME || '30s',
      stages: [
        { duration: __ENV.RAMP_UP || '1m', target: Number(__ENV.TARGET_VUS || 20) },
        { duration: __ENV.HOLD || '3m', target: Number(__ENV.TARGET_VUS || 20) },
        { duration: __ENV.RAMP_DOWN || '30s', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: smokeOptions.thresholds,
};

export default runJourney;

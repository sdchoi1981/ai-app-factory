import assert from 'node:assert/strict';
import test from 'node:test';
import { createDashboardData } from './build.mjs';

const app = {
  appId: 'example',
  appName: 'Example',
  description: 'Offline utility',
  platform: 'android',
  status: 'testing',
  versionName: '1.0',
  versionCode: 1
};
const run = {
  appId: 'example',
  versionName: '1.0',
  runId: 123,
  updatedAt: '2026-11-27T05:00:00Z',
  build: 'success',
  test: 'success',
  artifacts: { apk: 456 }
};

test('exposes only dashboard fields and leaves absent runs empty', () => {
  const data = createDashboardData({ apps: [{ ...app, buildConfiguration: { other: 'unused' } }] });
  assert.deepEqual(data.apps, [{ ...app, runs: [] }]);
});

test('sorts runs and derives repository URLs from integer IDs', () => {
  const earlier = { ...run, runId: 122, updatedAt: '2026-11-26T05:00:00Z', test: null };
  const [latest, previous] = createDashboardData([app], { runs: [earlier, run] }).apps[0].runs;
  const url = 'https://github.com/sdchoi1981/ai-app-factory/actions/runs/123';
  assert.equal(latest.url, url);
  assert.deepEqual(latest.artifacts, { apk: url + '/artifacts/456' });
  assert.equal(previous.test, null);
});

test('rejects invalid, foreign or duplicate app and run references', () => {
  assert.throws(() => createDashboardData([app, app]));
  for (const bad of [
    { ...run, appId: 'unknown' },
    { ...run, runId: 'https://other.example' },
    { ...run, build: 'invented' },
    { ...run, updatedAt: '2026-11-27' },
    { ...run, artifacts: { apk: 'https://other.example' } },
    { ...run, artifacts: { web: 456 } }
  ]) {
    assert.throws(() => createDashboardData([app], { runs: [bad] }));
  }
  assert.throws(() => createDashboardData([app], { runs: [run, run] }));
});

import assert from 'node:assert/strict';
import test from 'node:test';
import { ADMOB_ANDROID_TEST_UNITS, createAdController } from './ads/index.mjs';
import { createAnalytics } from './analytics/index.mjs';
import { createStorage, readSettings, requireIncreasingVersion } from './core/index.mjs';
import { commonMessages, resolveLanguage, translate } from './localization/index.mjs';
import { createConsentController, describePrivacy } from './privacy/index.mjs';

test('storage is scoped and version/settings validation fails on invalid input', () => {
  const backing = new Map();
  const backend = {
    getItem: key => backing.get(key) ?? null,
    setItem: (key, value) => backing.set(key, value),
    removeItem: key => backing.delete(key)
  };
  const a = createStorage('app-one', backend);
  const b = createStorage('app-two', backend);
  assert.equal(a.write('theme', 'dark'), true);
  assert.equal(a.read('theme', 'system'), 'dark');
  assert.equal(b.read('theme', 'system'), 'system');
  backing.set('app-one:theme', 'bad');
  assert.equal(a.read('theme', 'system'), 'system');
  assert.deepEqual(readSettings({ enabled: false }, { enabled: true, unknown: 1 }), { enabled: true });
  assert.throws(() => readSettings({ enabled: false }, { enabled: 'true' }), TypeError);
  assert.equal(requireIncreasingVersion(1, 2), 2);
  assert.throws(() => requireIncreasingVersion(2, 1), RangeError);
});

test('language resolution normalizes regional tags and prevents inherited lookup', () => {
  assert.equal(resolveLanguage(['fr', 'ja-JP'], commonMessages), 'ja');
  assert.equal(resolveLanguage('constructor', commonMessages), 'en');
  assert.equal(translate(commonMessages, 'ko', 'theme'), '테마');
  assert.equal(translate(commonMessages, 'en', 'constructor'), 'constructor');
});

test('ad consent is denied until refreshed and cannot be revived by a stale response', async () => {
  let done;
  let forms = 0;
  const controller = createConsentController({
    requestConsentInfoUpdate: () => new Promise(resolve => { done = resolve; }),
    loadAndShowConsentFormIfRequired: async () => { forms++; },
    canRequestAds: () => true
  });
  assert.equal(controller.canRequestAds(), false);
  const first = controller.refresh();
  controller.revoke();
  done();
  assert.equal(await first, false);
  assert.equal(forms, 0);
  const next = controller.refresh();
  done();
  assert.equal(await next, true);
  assert.equal(forms, 1);
  assert.equal(await controller.showPrivacyOptions(), false);
  assert.throws(() => describePrivacy({ policyUrl: 'http://example.com/' }), TypeError);
  assert.deepEqual(describePrivacy(), { policyUrl: null, dataCollected: [] });
});

test('only approved test placements show and the frequency cap applies', async () => {
  let now = 0;
  let allowed = false;
  const shown = [];
  const options = {
    consent: { canRequestAds: () => allowed },
    adapter: { show: async (...args) => { shown.push(args); return true; } },
    clock: () => now,
    minIntervalMs: 2000
  };
  assert.equal(await createAdController(options).show('banner'), false);
  const ads = createAdController({ ...options, enabled: true });
  assert.equal(await ads.show('banner'), false);
  allowed = true;
  assert.equal(await ads.show('banner'), true);
  assert.equal(await ads.show('banner'), false);
  assert.equal(await ads.show('constructor'), false);
  now += 2000;
  assert.equal(await ads.show('banner'), true);
  assert.deepEqual(shown, Array(2).fill(['banner', ADMOB_ANDROID_TEST_UNITS.banner]));
  assert.equal(await createAdController({
    ...options, enabled: true, mode: 'production', units: ADMOB_ANDROID_TEST_UNITS
  }).show('banner'), false);
});

test('analytics waits for per-purpose opt-in and is disabled by default', () => {
  const events = [];
  const options = {
    consent: () => ({ analytics: 'granted', diagnostics: 'denied' }),
    adapter: { usage: code => events.push(code), crash: code => events.push(code) }
  };
  assert.equal(createAnalytics(options).usage('opened'), false);
  const analytics = createAnalytics({ ...options, enabled: true });
  assert.equal(analytics.usage('opened'), true);
  assert.equal(analytics.crash('render_failure'), false);
  assert.deepEqual(events, ['opened']);
});

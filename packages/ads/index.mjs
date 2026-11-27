export const ADMOB_ANDROID_TEST_APP_ID = 'ca-app-pub-3940256099942544~3347511713';
export const ADMOB_ANDROID_TEST_UNITS = Object.freeze({
  banner: 'ca-app-pub-3940256099942544/6300978111',
  interstitial: 'ca-app-pub-3940256099942544/1033173712',
  rewarded: 'ca-app-pub-3940256099942544/5224354917'
});

export function createAdController({
  enabled = false, mode = 'test', enableProduction = false, units, consent, adapter,
  minIntervalMs = 60000, clock = Date.now
} = {}) {
  if (!Number.isFinite(minIntervalMs) || minIntervalMs < 0) {
    throw new RangeError('Invalid ad interval');
  }
  const last = new Map();
  let active = false;
  return {
    async show(placement) {
      if (!enabled || !adapter || consent?.canRequestAds() !== true ||
          !Object.hasOwn(ADMOB_ANDROID_TEST_UNITS, placement)) return false;
      const unit = mode === 'test' ? ADMOB_ANDROID_TEST_UNITS[placement]
        : mode === 'production' && enableProduction && Object.hasOwn(units ?? {}, placement)
          ? units[placement] : undefined;
      if (typeof unit !== 'string' || !/^ca-app-pub-\d{16}\/\d{10}$/.test(unit)) return false;
      const now = clock();
      if (active || now - (last.get(placement) ?? -Infinity) < minIntervalMs) return false;
      active = true;
      last.set(placement, now);
      try {
        // A native adapter must check consent again before showing a loaded ad.
        return await adapter.show(placement, unit) === true;
      } finally {
        active = false;
      }
    }
  };
}

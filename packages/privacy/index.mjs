export const Consent = Object.freeze({
  UNKNOWN: 'unknown',
  DENIED: 'denied',
  GRANTED: 'granted'
});

export function permits(consent, purpose) {
  return Object.hasOwn(consent ?? {}, purpose) && consent[purpose] === Consent.GRANTED;
}

export function describePrivacy({ policyUrl = null, dataCollected = [] } = {}) {
  if (policyUrl !== null && new URL(policyUrl).protocol !== 'https:') {
    throw new TypeError('The privacy policy URL must use HTTPS');
  }
  if (!Array.isArray(dataCollected) ||
      !dataCollected.every(entry => entry && [entry.type, entry.purpose].every(
        part => typeof part === 'string' && part.trim() !== ''))) {
    throw new TypeError('Each collection entry needs a type and purpose');
  }
  return { policyUrl, dataCollected: dataCollected.map(({ type, purpose }) => ({ type, purpose })) };
}

// A native bridge may implement this boundary using Google UMP. Request fresh
// consent information for each app launch, and provide a privacy options entry
// point when UMP requires one. Keep analytics opt-in separate from ad consent.
export function createConsentController(adapter) {
  let allowed = false;
  let revision = 0;
  return {
    canRequestAds: () => allowed,
    revoke() {
      revision++;
      allowed = false;
    },
    async refresh() {
      const current = ++revision;
      allowed = false;
      try {
        if (!adapter) return false;
        await adapter.requestConsentInfoUpdate();
        if (revision !== current) return false;
        await adapter.loadAndShowConsentFormIfRequired();
        if (revision === current) allowed = adapter.canRequestAds() === true;
      } catch {
        if (revision === current) allowed = false;
      }
      return revision === current && allowed;
    },
    async showPrivacyOptions() {
      const current = ++revision;
      allowed = false;
      try {
        if (!adapter) return false;
        await adapter.showPrivacyOptionsForm();
        if (revision === current) allowed = adapter.canRequestAds() === true;
      } catch {
        if (revision === current) allowed = false;
      }
      return revision === current && allowed;
    }
  };
}

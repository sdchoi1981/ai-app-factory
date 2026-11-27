import { permits } from '../privacy/index.mjs';

export function createAnalytics({ enabled = false, consent = () => null, adapter } = {}) {
  function report(purpose, method, value) {
    if (!enabled || !adapter || !permits(consent(), purpose)) return false;
    const handler = adapter[method];
    if (typeof handler !== 'function') return false;
    handler.call(adapter, value);
    return true;
  }
  return {
    // Callers define reviewed, bounded event codes. Keep user-entered text,
    // account identifiers and raw error objects out of analytics.
    usage(code) { return report('analytics', 'usage', code); },
    crash(code) { return report('diagnostics', 'crash', code); },
    revenue(event) { return report('revenue', 'revenue', event); }
  };
}

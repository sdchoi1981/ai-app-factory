export function createStorage(namespace, backend) {
  if (!/^[a-z][a-z0-9-]*$/.test(namespace)) throw new TypeError('Invalid storage namespace');
  const keyFor = key => `${namespace}:${key}`;
  return {
    read(key, fallback) {
      try {
        const raw = (backend ?? globalThis.localStorage).getItem(keyFor(key));
        return raw === null ? fallback : JSON.parse(raw);
      } catch {
        return fallback;
      }
    },
    write(key, value) {
      try {
        const raw = JSON.stringify(value);
        if (raw === undefined) return false;
        (backend ?? globalThis.localStorage).setItem(keyFor(key), raw);
        return true;
      } catch {
        return false;
      }
    },
    remove(key) {
      try {
        (backend ?? globalThis.localStorage).removeItem(keyFor(key));
        return true;
      } catch {
        return false;
      }
    }
  };
}

export function readSettings(defaults, overrides = {}) {
  return Object.fromEntries(Object.entries(defaults).map(([key, fallback]) => {
    const value = Object.hasOwn(overrides, key) ? overrides[key] : fallback;
    if (typeof value !== typeof fallback) throw new TypeError(`Invalid setting: ${key}`);
    return [key, value];
  }));
}

export function describeError(error) {
  return error instanceof Error ? error.message : String(error);
}

export function requireIncreasingVersion(current, next) {
  if (![current, next].every(code => Number.isSafeInteger(code) && code >= 1 && code <= 2100000000) ||
      next <= current) {
    throw new RangeError('Google Play version code must increase within 1–2100000000');
  }
  return next;
}

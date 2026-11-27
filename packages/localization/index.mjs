export const commonMessages = Object.freeze({
  ko: Object.freeze({ language: '언어', theme: '테마', auto: '시스템', light: '밝게', dark: '어둡게' }),
  en: Object.freeze({ language: 'Language', theme: 'Theme', auto: 'System', light: 'Light', dark: 'Dark' }),
  ja: Object.freeze({ language: '言語', theme: 'テーマ', auto: 'システム', light: 'ライト', dark: 'ダーク' })
});

export function resolveLanguage(preferred, messages, fallback = 'en') {
  for (const tag of Array.isArray(preferred) ? preferred : [preferred]) {
    if (typeof tag !== 'string') continue;
    const normalized = tag.toLowerCase();
    for (const key of [normalized, normalized.split('-')[0]]) {
      if (Object.hasOwn(messages, key)) return key;
    }
  }
  if (!Object.hasOwn(messages, fallback)) throw new TypeError('Missing fallback language');
  return fallback;
}

export function translate(messages, language, key, fallback = 'en') {
  for (const candidate of [language, fallback]) {
    if (Object.hasOwn(messages, candidate) && Object.hasOwn(messages[candidate], key)) {
      return messages[candidate][key];
    }
  }
  return key;
}

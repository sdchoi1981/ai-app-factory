import { createStorage } from './packages/core/index.mjs';
import { commonMessages, resolveLanguage, translate } from './packages/localization/index.mjs';
import { applyTheme } from './packages/ui/index.mjs';

const preferences = createStorage(document.querySelector('main').dataset.appId);
const messages = {
  ko: { ...commonMessages.ko, privacy: '설정은 이 기기에 저장됩니다.' },
  en: { ...commonMessages.en, privacy: 'Settings stay on this device.' },
  ja: { ...commonMessages.ja, privacy: '設定はこの端末に保存されます。' }
};

const language = document.getElementById('language');
const theme = document.getElementById('theme');
const translations = {
  'language-label': 'language', 'theme-label': 'theme', privacy: 'privacy'
};

function render() {
  const text = key => translate(messages, language.value, key);
  document.documentElement.lang = language.value;
  applyTheme(document.documentElement, theme.value);
  for (const [id, key] of Object.entries(translations)) {
    document.getElementById(id).textContent = text(key);
  }
  for (const option of theme.options) {
    option.textContent = text(option.value === 'system' ? 'auto' : option.value);
  }
}

language.value = resolveLanguage([preferences.read('language', null), ...navigator.languages], messages);
const savedTheme = preferences.read('theme', 'system');
theme.value = ['light', 'dark'].includes(savedTheme) ? savedTheme : 'system';
language.addEventListener('change', () => {
  preferences.write('language', language.value);
  render();
});
theme.addEventListener('change', () => {
  preferences.write('theme', theme.value);
  render();
});
render();

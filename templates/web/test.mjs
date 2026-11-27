import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import { createStorage } from '../../packages/core/index.mjs';
import { commonMessages, resolveLanguage } from '../../packages/localization/index.mjs';
import { applyTheme } from '../../packages/ui/index.mjs';

test('Web shell exposes theme, language and accessible mobile layout', async () => {
  const html = await readFile(new URL('src/index.html', import.meta.url), 'utf8');
  assert.match(html, /meta name="viewport"/);
  assert.match(html, /data-app-id="__APP_ID__"/);
  assert.match(html, /packages\/ui\/styles\.css/);
  for (const tag of ['en', 'ko', 'ja']) {
    assert.ok(html.includes(`value="${tag}" lang="${tag}"`));
    assert.ok(commonMessages[tag].language);
  }
  assert.equal(resolveLanguage(['fr', 'ja-JP'], commonMessages), 'ja');
});

test('Theme preferences survive storage and handle invalid/stale values', () => {
  const backing = new Map();
  const backend = {
    getItem: key => backing.get(key) ?? null,
    setItem: (key, value) => backing.set(key, value)
  };
  const storage = createStorage('__APP_ID__', backend);
  assert.equal(storage.read('theme', 'system'), 'system');
  assert.equal(storage.write('theme', 'dark'), true);
  const root = { dataset: {} };
  applyTheme(root, storage.read('theme', 'system'));
  assert.equal(root.dataset.theme, 'dark');
  applyTheme(root, 'invalid');
  assert.deepEqual(root.dataset, {});
});

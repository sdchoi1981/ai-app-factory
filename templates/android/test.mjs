import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const read = path => readFile(new URL(path, import.meta.url), 'utf8');

test('Android package and platform settings', async () => {
  const [config, activity] = await Promise.all([
    read('app/build.gradle'), read('app/src/main/java/factory/starter/MainActivity.java')
  ]);
  assert.match(config, /applicationId '__APPLICATION_ID__'/);
  assert.match(config, /compileSdk 36/);
  assert.match(config, /targetSdk 36/);
  assert.match(config, /JavaVersion\.VERSION_17/);
  assert.match(config, /packages\/core\/android/);
  assert.match(config, /packages\/ui\/android/);
  assert.match(activity, /\bpackage\s+([a-z][a-z0-9_]*\.)+[a-z][a-z0-9_]*;/i);
});

test('Launcher has no network permission and all three translations exist', async () => {
  const manifest = await read('app/src/main/AndroidManifest.xml');
  assert.match(manifest, /android\.intent\.category\.LAUNCHER/);
  assert.match(manifest, /android:usesCleartextTraffic="false"/);
  assert.doesNotMatch(manifest, /android\.permission\.INTERNET/);
  for (const suffix of ['', '-ko', '-ja']) {
    const strings = await read(`app/src/main/res/values${suffix}/strings.xml`);
    assert.match(strings, /name="increment"/);
    assert.match(strings, /name="count">[^<]*%1\$d/);
  }
});

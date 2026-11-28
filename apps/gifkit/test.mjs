import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const manifest = readFileSync('app/src/main/AndroidManifest.xml', 'utf8');
const source = readFileSync('app/src/main/java/factory/kit/MainActivity.java', 'utf8');
const build = readFileSync('app/build.gradle', 'utf8');
const locales = readFileSync('app/src/main/res/xml/locales_config.xml', 'utf8');
assert.doesNotMatch(manifest, /<uses-permission/);
assert.match(manifest, /android:allowBackup="false"/);
assert.match(build, /applicationId 'com\.factory\.gifkit'/);
for (const language of ['ko', 'en', 'ja']) assert.ok(locales.includes(`android:name="${language}"`));
assert.match(source, /Intent\.ACTION_OPEN_DOCUMENT/);
assert.match(source, /Intent\.ACTION_CREATE_DOCUMENT/);
assert.match(source, /frames\.add\(index - 1, frames\.remove\(index\)\)/);
assert.match(source, /gif\.frame\(pixels, milliseconds\)/);
assert.match(source, /ui\.preview\(image\)/);
console.log('GifKit: privacy, locale, ordered preview and export wiring checked');

const directory = mkdtempSync(join(tmpdir(), 'gifkit-'));
try {
  execFileSync('javac', ['-d', directory, 'app/src/main/java/factory/kit/GifWriter.java', 'test/GifWriterTest.java'], { stdio: 'inherit' });
  execFileSync('java', ['-cp', directory, 'factory.kit.GifWriterTest'], { stdio: 'inherit' });
} finally {
  rmSync(directory, { recursive: true, force: true });
}

import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const manifest = readFileSync('app/src/main/AndroidManifest.xml', 'utf8');
const source = readFileSync('app/src/main/java/factory/kit/MainActivity.java', 'utf8');
const build = readFileSync('app/build.gradle', 'utf8');
const locales = readFileSync('app/src/main/res/xml/locales_config.xml', 'utf8');
assert.doesNotMatch(manifest, /<uses-permission|android:allowBackup="true"/);
assert.match(manifest, /android:allowBackup="false"/);
assert.match(build, /applicationId 'com\.factory\.cleankit'/);
for (const language of ['ko', 'en', 'ja']) assert.ok(locales.includes(`android:name="${language}"`));
assert.match(source, /Intent\.ACTION_OPEN_DOCUMENT/);
assert.match(source, /new DuplicateIndex<>\(\)/);
assert.doesNotMatch(source, /openOutputStream|ACTION_CREATE_DOCUMENT|deleteDocument/);
assert.match(source, /ui\.preview\(thumbnail\)/);
console.log('CleanKit: read-only picker, privacy, locale and preview wiring checked');

const directory = mkdtempSync(join(tmpdir(), 'cleankit-'));
try {
  execFileSync('javac', ['-d', directory, 'app/src/main/java/factory/kit/DuplicateIndex.java', 'test/DuplicateIndexTest.java'], { stdio: 'inherit' });
  execFileSync('java', ['-cp', directory, 'factory.kit.DuplicateIndexTest'], { stdio: 'inherit' });
} finally {
  rmSync(directory, { recursive: true, force: true });
}

import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const manifest = readFileSync('app/src/main/AndroidManifest.xml', 'utf8');
const source = readFileSync('app/src/main/java/factory/kit/MainActivity.java', 'utf8');
const build = readFileSync('app/build.gradle', 'utf8');
assert.doesNotMatch(manifest, /<uses-permission\b/);
assert.match(manifest, /android:allowBackup="false"/);
assert.match(manifest, /@style\/FactoryTheme/);
assert.ok(build.includes("applicationId 'com.factory.measurekit'"));
assert.match(source, /TYPE_GRAVITY/);
assert.match(source, /TYPE_ACCELEROMETER/);
assert.match(source, /unregisterListener\(this\)/);
assert.match(source, /String t\(String en, String ko, String ja\)/);
assert.doesNotMatch(source, /ACTION_OPEN_DOCUMENT|ACTION_CREATE_DOCUMENT|FileOutputStream/);
const directory = mkdtempSync(join(tmpdir(), 'measurekit-'));
try {
  execFileSync('javac', ['-d', directory, 'app/src/main/java/factory/kit/LevelMath.java', 'test/LevelMathTest.java'], { stdio: 'inherit' });
  execFileSync('java', ['-cp', directory, 'factory.kit.LevelMathTest'], { stdio: 'inherit' });
} finally {
  rmSync(directory, { recursive: true, force: true });
}

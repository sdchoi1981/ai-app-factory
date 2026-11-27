import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const manifest = readFileSync('app/src/main/AndroidManifest.xml', 'utf8');
const source = readFileSync('app/src/main/java/factory/fun/MainActivity.java', 'utf8');
const build = readFileSync('app/build.gradle', 'utf8');
assert.doesNotMatch(manifest, /<uses-permission\b/);
assert.match(manifest, /android:allowBackup="false"/);
assert.match(manifest, /@style\/FactoryTheme/);
assert.ok(build.includes("applicationId 'com.factory.ledkit'"));
assert.match(source, /String t\(String en, String ko, String ja\)/);
assert.match(source, /hasWindowFocus\(\)\) postInvalidateOnAnimation\(\)/);
assert.match(source, /setEnabled\(direction != 2\)/);
const directory = mkdtempSync(join(tmpdir(), 'ledkit-'));
try {
  execFileSync('javac', ['-d', directory, 'app/src/main/java/factory/fun/Rules.java', 'test/RulesTest.java'], { stdio: 'inherit' });
  execFileSync('java', ['-cp', directory, 'factory.fun.RulesTest'], { stdio: 'inherit' });
} finally {
  rmSync(directory, { recursive: true, force: true });
}

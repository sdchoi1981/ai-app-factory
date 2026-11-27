import assert from 'node:assert/strict';import {readFileSync,mkdtempSync,rmSync} from 'node:fs';import {execFileSync} from 'node:child_process';import {tmpdir} from 'node:os';import {join} from 'node:path';
const manifest=readFileSync('app/src/main/AndroidManifest.xml','utf8'),source=readFileSync('app/src/main/java/factory/kit/MainActivity.java','utf8'),build=readFileSync('app/build.gradle','utf8');assert.ok(!manifest.includes('android.permission.INTERNET'));assert.ok(!manifest.includes('READ_MEDIA_IMAGES'));assert.ok(manifest.includes('android:allowBackup="false"'));assert.ok(build.includes("applicationId 'com.factory.scankit'"));assert.ok(source.includes("ACTION_IMAGE_CAPTURE"));assert.ok(source.includes('String t(String en,String ko,String ja)'));console.log('ScanKit: manifest, package and feature wiring checked');

assert.match(source, /new KitUi\(/);
assert.match(source, /SystemInsets.apply\(this,scroll\)/);
assert.match(source, /FLAG_SECURE/);
assert.match(source, /ACCESSIBILITY_LIVE_REGION_POLITE/);
assert.match(source, /closed\|\|ticket!=previewGeneration/);
assert.match(source, /DocumentsContract.deleteDocument/);
assert.match(manifest, /@style\/FactoryTheme/);
assert.match(source, /pageIndex\+1<selected.size\(\)/);
assert.match(source, /preview.setColorFilter/);
assert.match(source, /clearCaptured\(\);selected.clear\(\)/);
assert.match(manifest, /android:authorities="\$\{applicationId\}.capture" android:exported="false"/);

import assert from 'node:assert/strict';import {readFileSync,mkdtempSync,rmSync} from 'node:fs';import {execFileSync} from 'node:child_process';import {tmpdir} from 'node:os';import {join} from 'node:path';
const manifest=readFileSync('app/src/main/AndroidManifest.xml','utf8'),source=readFileSync('app/src/main/java/factory/kit/MainActivity.java','utf8'),build=readFileSync('app/build.gradle','utf8');assert.ok(!manifest.includes('android.permission.INTERNET'));assert.ok(!manifest.includes('READ_MEDIA_IMAGES'));assert.ok(manifest.includes('android:allowBackup="false"'));assert.ok(build.includes("applicationId 'com.factory.photokit'"));assert.ok(source.includes("jpegLimit"));assert.ok(source.includes('String t(String en,String ko,String ja)'));console.log('PhotoKit: manifest, package and feature wiring checked');

assert.match(source,/image\(u,640\)/);assert.match(source,/ticket!=previewGeneration/);assert.match(source,/completed\.length\(\)/);assert.match(source,/setWorking\(true\)/);assert.match(manifest,/@style\/FactoryTheme/);console.log('Bounded preview, output size and theme wiring verified');

assert.match(source,/selected\.get\(previewIndex\)/);
assert.match(source,/target<0\|\|target>=selected\.size\(\)/);
assert.match(source,/DocumentsContract\.deleteDocument/);
console.log('Batch preview navigation and incomplete document cleanup wiring verified');

assert.match(source, /private void showWorkspace\(boolean output\)/);
assert.match(source, /photoWorkspace\.setVisibility\(output\?View\.GONE:View\.VISIBLE\)/);
assert.match(source, /outputTab\.setEnabled\(!busy&&!selected\.isEmpty\(\)\)/);
assert.match(source, /ValueAnimator\.areAnimatorsEnabled\(\)/);
assert.match(source, /setImageResource\(R\.drawable\.photo_workbench\)/);
assert.match(source, /previewFeedback\.setAccessibilityLiveRegion/);
console.log('Contextual photo and output navigation, empty state artwork and motion controls checked');

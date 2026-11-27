import assert from 'node:assert/strict';import {execFileSync} from 'node:child_process';import {mkdtempSync,rmSync,readFileSync} from 'node:fs';import {tmpdir} from 'node:os';import {join} from 'node:path';const dir=mkdtempSync(join(tmpdir(),'passportkit-'));try{execFileSync('javac',['-encoding','UTF-8','-d',dir,'app/src/main/java/com/factory/passportkit/PhotoRules.java','app/src/main/java/com/factory/passportkit/BackgroundEdit.java','test/PhotoRulesTest.java'],{stdio:'inherit'});execFileSync('java',['-cp',dir,'PhotoRulesTest'],{stdio:'inherit'});const manifest=readFileSync('app/src/main/AndroidManifest.xml','utf8');assert.ok(manifest.includes('android.permission.INTERNET" tools:node="remove"'));assert.ok(!manifest.includes('READ_MEDIA_IMAGES'));assert.ok(manifest.includes('android:exported="false" android:grantUriPermissions="true"'));assert.ok(readFileSync('app/build.gradle','utf8').includes('com.google.mlkit:face-detection:16.1.7'));}finally{rmSync(dir,{recursive:true,force:true})}
const root='app/src/main/res/';const ids=s=>[...s.matchAll(/<string name="([^"]+)"/g)].map(m=>m[1]).sort();const baseline=ids(readFileSync(root+'values/strings.xml','utf8'));assert.equal(baseline.length,98);for(const l of ['values-ko','values-ja','values-b+zh+Hans','values-b+zh+Hant']){assert.deepEqual(ids(readFileSync(root+l+'/strings.xml','utf8')),baseline);}console.log('Five locale string resource coverage verified');

const launchManifest=readFileSync('app/src/main/AndroidManifest.xml','utf8');assert.match(launchManifest,/android:name="\.MainActivity" android:theme="@style\/PassportLaunchTheme"/);const activity=readFileSync('app/src/main/java/com/factory/passportkit/MainActivity.java','utf8');assert.match(activity,/setTheme\(R.style.FactoryTheme\);\s*super.onCreate\(saved\);/);assert.ok(readFileSync(root+'values-v31/passport_splash.xml','utf8').includes('android:windowSplashScreenAnimatedIcon'));assert.ok(readFileSync(root+'values/passport_splash.xml','utf8').includes('@drawable/passport_splash_background'));console.log('Native startup themes verified (API 26–30 and 31+)');

assert.ok(!readFileSync(root+'values/passport_splash.xml','utf8').includes('android:windowLightNavigationBar'));assert.ok(readFileSync(root+'values-v31/passport_splash.xml','utf8').includes('android:windowLightNavigationBar'));

assert.match(activity,/SPLASH_MIN_MS=1500L/);assert.match(activity,/!firstActivityCreated&&saved==null/);assert.match(activity,/holdSplashUntil\(scroll,startedAt\+SPLASH_MIN_MS\)/);assert.match(activity,/uptimeMillis\(\)<deadline\)return false/);assert.match(activity,/splashHandler\.postAtTime\(releaseSplash,deadline\)/);assert.match(activity,/onPause\(\)\{releaseSplashGate\(\);super\.onPause\(\);\}/);assert.match(activity,/onDestroy\(\)\{releaseSplashGate\(\);/);assert.doesNotMatch(activity,/Thread\.sleep\(/);console.log('Cold-start splash timing and lifecycle wiring verified');

assert.match(activity,/showStage\(1\)/);assert.match(activity,/class PrintPreview extends View/);assert.match(activity,/if\(next!=0&&source==null\)next=0/);assert.match(activity,/FLAG_SECURE/);console.log('Stage wiring, preview and privacy controls verified');

const gallery = readFileSync('app/src/main/java/com/factory/passportkit/GalleryExport.java', 'utf8');
assert.deepEqual([...launchManifest.matchAll(/<uses-permission android:name="android\.permission\.WRITE_EXTERNAL_STORAGE"([^>]*)>/g)].map(match => match[1]), [' android:maxSdkVersion="28"/']);
assert.match(readFileSync(root + 'xml/gallery_paths.xml', 'utf8'), /<external-path name="passportkit-pictures" path="Pictures\/PassportKit\/"\s*\/>/);
assert.match(gallery, /IS_PENDING, 1/);
assert.match(gallery, /IS_PENDING, 0/);
assert.match(gallery, /resolver\.delete\(uri, null, null\)/);
assert.match(activity, /DocumentsContract\.deleteDocument\(getContentResolver\(\),destination\)/);
assert.match(activity, /new Intent\(Intent\.ACTION_CREATE_DOCUMENT\)\.setType\("application\/pdf"\)/);
assert.match(activity, /Intent\.FLAG_GRANT_READ_URI_PERMISSION/);
console.log('Gallery export permissions, scoped sharing and PDF picker wiring verified');

assert.match(activity,/root\.addView\(scroll,new LinearLayout\.LayoutParams\(-1,0,1\)\)/);
assert.match(activity,/root\.addView\(footer,new LinearLayout\.LayoutParams\(-1,-2\)\)/);
assert.match(activity,/if\(guideSelected\)showStage\(2\);/);
assert.match(activity,/cropTools\.setVisibility\(guides\?View\.GONE:View\.VISIBLE\)/);
console.log('Fixed workflow controls and crop/guide workspaces verified');

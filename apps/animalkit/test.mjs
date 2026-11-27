import assert from 'node:assert/strict';import {readFileSync,mkdtempSync,rmSync} from 'node:fs';import {execFileSync} from 'node:child_process';import {tmpdir} from 'node:os';import {join} from 'node:path';
const manifest=readFileSync('app/src/main/AndroidManifest.xml','utf8'),source=readFileSync('app/src/main/java/factory/fun/MainActivity.java','utf8'),build=readFileSync('app/build.gradle','utf8');assert.ok(!manifest.includes('android.permission.INTERNET'));assert.ok(manifest.includes('android:allowBackup="false"'));assert.ok(build.includes("applicationId 'com.factory.animalkit'"));assert.ok(source.includes('String t(String en,String ko,String ja)'));const dir=mkdtempSync(join(tmpdir(),'animalkit-'));try{execFileSync('javac',['-encoding','UTF-8','-d',dir,'app/src/main/java/factory/fun/Rules.java','test/RulesTest.java'],{stdio:'inherit'});execFileSync('java',['-cp',dir,'factory.fun.RulesTest'],{stdio:'inherit'});}finally{rmSync(dir,{recursive:true,force:true});}

assert.match(manifest, /android:theme="@style\/FactoryTheme"/);
assert.ok(!manifest.includes('<uses-permission'));
assert.ok(source.includes('SystemInsets.apply(this,scroll)'));
assert.ok(source.includes('ui.preview('));

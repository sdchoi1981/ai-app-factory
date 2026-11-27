import assert from 'node:assert/strict';
import test from 'node:test';
import { extractRuns } from './collect-actions.mjs';
const apps=[{appId:'unitkit',appName:'UnitKit',versionName:'1.1',platform:'android'}];
const run={id:9,updated_at:'2026-11-27T07:00:00Z'};
test('uses per-app job result and only unexpired matching artifacts',()=>{
 const rows=extractRuns(apps,run,[{name:'UnitKit Android',status:'completed',conclusion:'success'}],[{name:'unitkit-UnitKit-1.1-debug-APK',id:10,expired:false},{name:'unitkit-UnitKit-1.1-unsigned-release-AAB',id:11,expired:true}], 'android');
 assert.equal(rows[0].build,'success');assert.deepEqual(rows[0].artifacts,{apk:10});
});
test('does not attribute skipped or absent matrix jobs to an app',()=>{
 assert.deepEqual(extractRuns(apps,run,[{name:'UnitKit Android',status:'completed',conclusion:'skipped'}],[],'android'),[]);
 assert.deepEqual(extractRuns(apps,run,[{name:'select / registry',status:'completed',conclusion:'success'}],[],'android'),[]);
});
test('records failed builds without inventing artifacts',()=>{
 const [row]=extractRuns(apps,run,[{name:'UnitKit Android',status:'completed',conclusion:'failure'}],[],'android');
 assert.equal(row.build,'failure');assert.deepEqual(row.artifacts,{});
});

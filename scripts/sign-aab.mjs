import { execFileSync } from 'node:child_process';
import { copyFileSync, constants, lstatSync, mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { getApp, appDirectory, root } from './registry.mjs';

const [id, output, ...extra] = process.argv.slice(2);
if (!id || !output || extra.length) throw Error('Usage: node scripts/sign-aab.mjs APP_ID OUTPUT.aab');
const app = getApp(id);
if (app.platform !== 'android') throw Error('Select an Android app.');
const target = resolve(output);
if (!target.endsWith('.aab') || target.startsWith(resolve(root) + '/')) throw Error('Output must be an AAB outside the repository.');
const input = join(appDirectory(app), 'app/build/outputs/bundle/release/app-release.aab');
const key = process.env.ANDROID_KEYSTORE_PATH;
const alias = process.env.ANDROID_KEY_ALIAS;
for (const name of ['ANDROID_KEYSTORE_PATH','ANDROID_KEY_ALIAS','ANDROID_STORE_PASSWORD','ANDROID_KEY_PASSWORD']) {
 if (!process.env[name]) throw Error('Missing signing setting: ' + name);
}
if (!lstatSync(input).isFile() || !lstatSync(key).isFile()) throw Error('Missing regular AAB or keystore file.');
if (lstatSync(target,{throwIfNoEntry:false})) throw Error('Output already exists; choose a new path.');
const temp = mkdtempSync(join(tmpdir(),'factory-sign-'));
const signed = join(temp,'signed.aab');
try {
 copyFileSync(input,signed,constants.COPYFILE_EXCL);
 const run = (command,args,env=process.env)=>execFileSync(command,args,{encoding:'utf8',env,maxBuffer:16*1024*1024,stdio:['ignore','pipe','pipe']});
 const bin = name=>process.env.JAVA_HOME?join(process.env.JAVA_HOME,'bin',name):name;
 // Refuse to add a second signature. The source must be the unsigned release build.
 const listing=run(bin('jar'),['tf',signed]);
 if(listing.split(/\r?\n/).some(x=>/^META-INF\/(?:[^/]+\.(?:SF|RSA|DSA|EC)|SIG-[^/]+)$/i.test(x))) throw Error('Source AAB already contains a signature.');
 run(bin('jarsigner'),['-keystore',key,'-storepass:env','ANDROID_STORE_PASSWORD','-keypass:env','ANDROID_KEY_PASSWORD',signed,alias]);
 const report=run(bin('jarsigner'),['-J-Duser.language=en','-J-Duser.country=US','-verify','-verbose',signed]);
 if(!/jar verified\./i.test(report)||/unsigned entries/i.test(report)) throw Error('AAB signature verification did not pass.');
 copyFileSync(signed,target,constants.COPYFILE_EXCL);
 console.log('Verified signed AAB created for '+app.appId+'. No upload performed.');
}finally{rmSync(temp,{recursive:true,force:true});}

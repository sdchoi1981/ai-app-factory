import { readFileSync, writeFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
const root=new URL('./',import.meta.url);
const photos=JSON.parse(readFileSync(new URL('photos.json',root),'utf8'));
const htmlPath=new URL('app/src/main/assets/index.html',root);
const html=readFileSync(htmlPath,'utf8');
const entries=JSON.parse(html.match(/const SNAKES=(\[.*?\]);\nconst GROUPS/s)[1]);
const data={};
const headers={'User-Agent':'SnakeKit educational offline photo build (https://github.com/sdchoi1981/ai-app-factory)'};
async function get(url,host){
 const u=new URL(url);
 if(u.protocol!=='https:'||u.hostname!==host)throw Error('Unapproved image host');
 const response=await fetch(u,{headers,redirect:'error',signal:AbortSignal.timeout(40000)});
 if(!response.ok)throw Error('HTTP '+response.status+' for '+u.pathname);
 return response;
}
for(const p of photos){
 if(!entries.some(s=>s.id===p.id)||data[p.id])throw Error('Unexpected photo ID');
 const url=new URL('https://commons.wikimedia.org/w/api.php');
 url.search=new URLSearchParams({action:'query',format:'json',formatversion:'2',titles:'File:'+p.title,prop:'imageinfo',iiprop:'url|mime|size|sha1|timestamp|extmetadata',iilimit:'1'});
 const j=await(await get(url,'commons.wikimedia.org')).json();
 const info=j.query?.pages?.[0]?.imageinfo?.[0];
 if(!info||info.mime!=='image/jpeg'||info.size>2000000||info.extmetadata?.LicenseShortName?.value!==p.license||info.timestamp.slice(0,16)!==p.timestamp.slice(0,16))throw Error('Photo source changed: '+p.title);
 const bytes=Buffer.from(await(await get(info.url,'upload.wikimedia.org')).arrayBuffer());
 const hash=createHash('sha1').update(bytes).digest('hex');
 const apiHash=BigInt('0x'+hash).toString(36).padStart(31,'0');
 if(bytes.length!==info.size||bytes[0]!==255||bytes[1]!==216||(info.sha1!==apiHash&&info.sha1!==hash)||(p.sha1&&p.sha1!==hash))throw Error('Photo integrity failure: '+p.title);
 const source='https://commons.wikimedia.org/wiki/File:'+encodeURIComponent(p.title.replaceAll(' ','_'));
 data[p.id]={data:'data:image/jpeg;base64,'+bytes.toString('base64'),credit:p.author+' · '+p.license+' · Wikimedia Commons · '+source+' · '+p.licenseUrl};
 console.log('Verified '+p.id+' '+hash);
}
if(!html.includes('const PHOTO_DATA={};'))throw Error('Unexpected photo placeholder');
writeFileSync(htmlPath,html.replace('const PHOTO_DATA={};','const PHOTO_DATA='+JSON.stringify(data).replaceAll('<','\\u003c')+';'));
console.log('Bundled '+Object.keys(data).length+' licensed photographs');

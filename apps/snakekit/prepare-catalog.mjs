import {readFileSync,writeFileSync} from 'node:fs';
const root=new URL('./',import.meta.url);
const file=new URL('app/src/main/assets/index.html',root);
const html=readFileSync(file,'utf8');
const match=html.match(/const SNAKES=(\[.*?\]);\nconst GROUPS/s);
if(!match)throw Error('Missing catalog');
const existing=JSON.parse(match[1]);
if(existing.length!==100)throw Error('Expected 100 base entries');
const names=new Set(existing.map(s=>s.scientific.replace(/ \(.*\)$/,'').toLowerCase()));
const ids=new Set(existing.map(s=>s.id));
const backbone='d7dddbf4-2cf0-4f39-9b2a-bb099caae36c';
const groups=[
 ['Micrurus','elapid'],['Naja','elapid'],['Bothrops','viper'],['Trimeresurus','viper'],
 ['Atheris','viper'],['Bitis','viper'],['Bungarus','elapid'],['Vipera','viper'],
 ['Gloydius','viper'],['Echis','viper'],['Pseudonaja','elapid'],['Pseudechis','elapid'],
 ['Python','python'],['Morelia','python'],['Lampropeltis','other'],['Thamnophis','other'],
 ['Lycodon','other'],['Atractus','other'],['Dipsas','other'],['Sibon','other'],
 ['Boiga','other'],['Oligodon','other'],['Hebius','other'],['Nerodia','other']
];
const added=[];
async function get(path){
 const url=new URL(path,'https://api.gbif.org/v1/');
 if(url.hostname!=='api.gbif.org'||url.protocol!=='https:')throw Error('Unexpected host');
 let failure;
 for(let attempt=0;attempt<3;attempt++){
  try{
   const r=await fetch(url,{headers:{'User-Agent':'SnakeKit offline catalog (https://github.com/sdchoi1981/ai-app-factory)'},redirect:'error',signal:AbortSignal.timeout(25000)});
   if(!r.ok)throw Error('HTTP '+r.status);
   return await r.json();
  }catch(e){failure=e;if(attempt<2)await new Promise(r=>setTimeout(r,1000*(attempt+1)))}
 }
 throw Error(url.pathname+': '+failure.message);
}
for(const [genus,group] of groups){
 if(added.length===400)break;
 const g=await get('species/match?rank=GENUS&strict=true&name='+encodeURIComponent(genus));
 if(g.rank!=='GENUS'||g.matchType!=='EXACT'||g.status!=='ACCEPTED'||g.canonicalName!==genus||g.class!=='Reptilia'||g.order!=='Squamata'||!Number.isSafeInteger(g.usageKey)){console.log('Skipped unmatched genus '+genus);continue;}
 let offset=0;const candidates=[];
 for(let page=0;page<10;page++){
  const result=await get('species/'+g.usageKey+'/children?limit=300&offset='+offset);
  if(!Array.isArray(result.results))throw Error('Invalid taxon page');
  for(const s of result.results){
   const name=s.canonicalName;
   if(s.rank!=='SPECIES'||s.taxonomicStatus!=='ACCEPTED'||s.genus!==genus||s.class!=='Reptilia'||s.order!=='Squamata'||s.extinct===true||s.datasetKey!==backbone||!Number.isSafeInteger(s.key)||typeof name!=='string'||!new RegExp('^'+genus+' [a-z][a-z-]+$').test(name))continue;
   candidates.push(s);
  }
  if(result.endOfRecords||!result.results.length)break;
  offset+=result.results.length;
 }
 candidates.sort((a,b)=>a.canonicalName.localeCompare(b.canonicalName,'en'));
 for(const s of candidates){
  const scientific=s.canonicalName,id='gbif-'+s.key;
  if(names.has(scientific.toLowerCase())||ids.has(id))continue;
  names.add(scientific.toLowerCase());ids.add(id);
  added.push({id,ko:scientific,en:scientific,scientific,group,venom:'unknown',region:'상세 분포 확인 중',habitat:'서식지 정보 확인 중',intro:'GBIF 분류 목록에 등록된 '+genus+' 속의 뱀이에요.',fact:'학명: '+scientific,pattern:'색과 무늬 확인 중',color:'#9aaa87',accent:'#627859',style:'plain',source:'https://www.gbif.org/species/'+s.key,basic:true,taxonomySource:'GBIF Backbone Taxonomy · CC BY 4.0 · https://doi.org/10.15468/39omei · https://creativecommons.org/licenses/by/4.0/'});
  if(added.length===400)break;
 }
 console.log(genus+': '+added.length+' new entries total');
}
if(added.length!==400)throw Error('Insufficient verified GBIF species: '+added.length);
const all=[...existing,...added];
writeFileSync(file,html.replace(match[0],'const SNAKES='+JSON.stringify(all).replaceAll('<','\\u003c')+';\nconst GROUPS'));
console.log('Bundled '+all.length+' catalog entries, '+added.length+' GBIF basic entries');

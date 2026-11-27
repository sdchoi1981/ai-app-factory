const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const html=fs.readFileSync('app/src/main/assets/index.html','utf8');
const {data,convert}=vm.runInNewContext(html.slice(html.indexOf('const data='),html.indexOf('// UI'))+';({data,convert})');
function c(cat,a,b,v,expected){const u=data[cat][2];const out=convert(v,u.find(x=>x[0]===a),u.find(x=>x[0]===b),cat);assert.ok(Math.abs(out-expected)<=Math.max(1,Math.abs(expected))*1e-10)}
c('length','in','cm',1,2.54);c('mass','lb','kg',1,.45359237);c('temp','°C','°F',100,212);c('temp','°F','°C',32,0);c('temp','K','°C',0,-273.15);c('area','평','m²',1,400/121);c('volume','gal US','L',1,3.785411784);c('speed','km/h','m/s',36,10);c('time','h','s',1,3600);c('data','MiB','B',1,1048576);c('pressure','atm','Pa',1,101325);c('energy','kWh','J',1,3600000);c('angle','deg','rad',180,Math.PI);c('power','kW','W',1,1000);assert.throws(()=>convert(-274,data.temp[2][0],data.temp[2][2],'temp'));
let n=15;for(const [cat,d] of Object.entries(data)){for(const u of d[2]){const base=d[2][0];assert.ok(Math.abs(convert(convert(10,u,base,cat),base,u,cat)-10)<1e-8);n++}}console.log(n+' tests passed');

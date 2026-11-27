const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const html=fs.readFileSync('app/src/main/assets/index.html','utf8');
const conversionScript = html.slice(html.indexOf('const data='), html.indexOf('// UI'));
const { data, convert, fmt, parseAmount } = vm.runInNewContext(
  conversionScript + ';({data,convert,fmt,parseAmount})',
);
function c(cat,a,b,v,expected){const u=data[cat][2];const out=convert(v,u.find(x=>x[0]===a),u.find(x=>x[0]===b),cat);assert.ok(Math.abs(out-expected)<=Math.max(1,Math.abs(expected))*1e-10)}
c('length','in','cm',1,2.54);c('mass','lb','kg',1,.45359237);c('temp','°C','°F',100,212);c('temp','°F','°C',32,0);c('temp','K','°C',0,-273.15);c('area','평','m²',1,400/121);c('volume','gal US','L',1,3.785411784);c('speed','km/h','m/s',36,10);c('time','h','s',1,3600);c('data','MiB','B',1,1048576);c('pressure','atm','Pa',1,101325);c('energy','kWh','J',1,3600000);c('angle','deg','rad',180,Math.PI);c('power','kW','W',1,1000);assert.throws(()=>convert(-274,data.temp[2][0],data.temp[2][2],'temp'));
let n=15;for(const [cat,d] of Object.entries(data)){for(const u of d[2]){const base=d[2][0];assert.ok(Math.abs(convert(convert(10,u,base,cat),base,u,cat)-10)<1e-8);n++}}

for (const [input, expected] of [
  ['  +12.5  ', 12.5],
  ['.5', 0.5],
  ['1.', 1],
  ['−0,5', -0.5],
  ['1,25e+2', 125],
  ['4.5e−3', 0.0045],
  ['-0', -0],
  [String(Number.MAX_VALUE), Number.MAX_VALUE],
]) {
  assert.equal(parseAmount(input), expected, input);
  n++;
}

for (const input of [
  '', ' ', '+', '.', ',', '1e', '1e309', 'NaN', 'Infinity', '0x10',
  '1 000', '1,000.5', '1,000,000', '42kg',
]) {
  assert.throws(() => parseAmount(input), { message: 'input' }, input);
  n++;
}

c('length', 'km', 'km', 1e308, 1e308);
c('length', 'km', 'mi', 1e308, (1000 / 1609.344) * 1e308);
c('temp', '°C', 'K', -273.15, 0);
const millimeters = data.length[2].find(([unit]) => unit === 'mm');
assert.equal(convert(Number.MIN_VALUE, millimeters, millimeters, 'length'), Number.MIN_VALUE);
const [grams, milligrams] = data.mass[2].slice(1, 3);
const subnormalGrams = 2.5e-321;
assert.equal(convert(subnormalGrams, grams, milligrams, 'mass'), subnormalGrams * 1000);
for (const value of [Number.MAX_VALUE, -Number.MAX_VALUE]) {
  const formatted = parseAmount(fmt(value));
  assert.ok(Math.abs(formatted - value) <= Math.abs(value) * 1e-11);
  n++;
}
n += 5;

console.log(n + ' tests passed');

import { readFile, writeFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';

const outcomes = new Set(['queued','in_progress','success','failure','cancelled','skipped']);
const state = job => outcomes.has(job.status) && job.status !== 'completed' ? job.status : outcomes.has(job.conclusion) ? job.conclusion : null;
export function extractRuns(apps, history, jobs, artifacts, platform, test = false) {
  const output = [];
  const used = new Set();
  for (const app of apps) {
    if (!test && app.platform !== platform) continue;
    const name = app.appName + (test ? ' tests' : platform === 'android' ? ' Android' : ' web');
    if (apps.filter(a => a.appName === app.appName && (test || a.platform === platform)).length !== 1) continue;
    const job = jobs.find(j => j.name === name);
    const result = job && state(job);
    if (!result || result === 'skipped' || used.has(app.appId)) continue;
    used.add(app.appId);
    const prefix = app.appId + '-' + app.appName + '-' + app.versionName + '-';
    const ids = {};
    for (const [key,suffix] of Object.entries(test ? {test:'test-results'} : platform === 'android' ? {apk:'debug-APK',aab:'unsigned-release-AAB'} : {web:'web'})) {
      const artifact = artifacts.find(a => a.name === prefix + suffix && !a.expired && Number.isSafeInteger(a.id) && a.id > 0);
      if (artifact && (test || result === 'success')) ids[key] = artifact.id;
    }
    output.push({ appId:app.appId,versionName:app.versionName,runId:history.id,updatedAt:history.updated_at,build:result,artifacts:ids });
  }
  return output;
}

async function collect() {
  const token = process.env.GH_TOKEN;
  const repo = process.env.GITHUB_REPOSITORY;
  if (!token || repo !== 'sdchoi1981/ai-app-factory') throw Error('Run from the authorized repository with a read-only token.');
  const root = 'https://api.github.com/repos/' + repo;
  async function get(path) {
    const response = await fetch(root + path, {headers:{Authorization:'Bearer ' + token,Accept:'application/vnd.github+json','X-GitHub-Api-Version':'2022-11-28'},signal:AbortSignal.timeout(30000),redirect:'error'});
    if (!response.ok) throw Error('GitHub API request failed: HTTP ' + response.status);
    return response.json();
  }
  async function pages(path, field) {
    const out = [];
    for (let p=1;p<=10;p++) {
      const d = await get(path + (path.includes('?')?'&':'?') + 'per_page=100&page=' + p);
      out.push(...d[field]);
      if (d[field].length < 100) return out;
    }
    throw Error('API pagination exceeded safety limit');
  }
  const current = JSON.parse(await readFile('apps/registry.json','utf8')).apps;
  const allowed = new Set(current.map(a=>a.appId));
  const registryCache = new Map();
  async function registry(sha) {
    if (!/^[a-f0-9]{40}$/.test(sha)) throw Error('Invalid commit SHA');
    if (!registryCache.has(sha)) {
      const d = await get('/contents/apps/registry.json?ref=' + sha);
      if (d.encoding !== 'base64') throw Error('Unsupported registry encoding');
      registryCache.set(sha,JSON.parse(Buffer.from(d.content,'base64').toString('utf8')).apps);
    }
    return registryCache.get(sha);
  }
  const rows=[], testRows=new Map();
  for (const [workflow,platform,isTest] of [['test-apps.yml',null,true],['build-android.yml','android',false],['build-web.yml','web',false]]) {
    const result = await get('/actions/workflows/' + workflow + '/runs?branch=main&per_page=20');
    for (const run of result.workflow_runs) {
      if (run.head_branch !== 'main' || !['push','workflow_dispatch'].includes(run.event)) continue;
      const apps = await registry(run.head_sha);
      const jobs = await pages('/actions/runs/' + run.id + '/jobs','jobs');
      const artifacts = await pages('/actions/runs/' + run.id + '/artifacts','artifacts');
      for (const row of extractRuns(apps,run,jobs,artifacts,platform,isTest)) {
        if (!allowed.has(row.appId)) continue;
        const key=run.head_sha+':'+row.appId+':'+row.versionName;
        if (isTest) {if (!testRows.has(key)) testRows.set(key,row.build);}
        else rows.push({...row,test:testRows.get(key) ?? null});
      }
    }
  }
  await writeFile(process.argv[2] || 'dashboard-results.json',JSON.stringify({runs:rows},null,2)+'\n');
  console.log('Collected '+rows.length+' verified app build entries.');
}
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) await collect();

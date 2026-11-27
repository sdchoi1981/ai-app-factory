import { copyFile, mkdir, readFile, writeFile } from 'node:fs/promises';
import { fileURLToPath, pathToFileURL } from 'node:url';

const root = new URL('./', import.meta.url);
const platforms = new Set(['android', 'web']);
const stages = new Set(['development', 'testing', 'released']);
const outcomes = new Set(['queued', 'in_progress', 'success', 'failure', 'cancelled', 'skipped']);
const repositoryUrl = 'https://github.com/sdchoi1981/ai-app-factory';

const isText = (value, limit) => typeof value === 'string' && value.length > 0 && value.length <= limit;
const isId = value => Number.isSafeInteger(value) && value > 0;
const readJson = async file => JSON.parse(await readFile(file, 'utf8'));

function normalizeApps(registry) {
  const source = Array.isArray(registry) ? registry : registry?.apps;
  if (!Array.isArray(source)) throw new Error('앱 레지스트리 형식이 올바르지 않습니다.');
  const apps = new Map();
  for (const app of source) {
    if (!app || !isText(app.appId, 64) || !/^[a-z][a-z0-9-]*$/.test(app.appId) ||
        !isText(app.appName, 120) || !isText(app.versionName, 48) ||
        !platforms.has(app.platform) || !stages.has(app.status) || apps.has(app.appId)) {
      throw new Error('앱 레지스트리의 ID, 이름, 버전, 플랫폼 또는 상태가 올바르지 않습니다.');
    }
    apps.set(app.appId, {
      appId: app.appId,
      appName: app.appName,
      description: isText(app.description, 1200) ? app.description : '',
      platform: app.platform,
      status: app.status,
      versionName: app.versionName,
      versionCode: isId(app.versionCode) ? app.versionCode : null,
      ...(app.monetization?.testBannerPrepared === true ? { testBannerPrepared: true } : {})
    });
  }
  return apps;
}

function normalizeRuns(apps, snapshot) {
  if (!Array.isArray(snapshot?.runs)) throw new Error('Actions 결과 입력에는 runs 배열이 필요합니다.');
  const runs = new Map();
  const seen = new Set();
  for (const run of snapshot.runs) {
    if (!run || !apps.has(run.appId) || !isId(run.runId) || !isText(run.versionName, 48) ||
        !isText(run.updatedAt, 40) ||
        !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,3})?Z$/.test(run.updatedAt) ||
        !Number.isFinite(Date.parse(run.updatedAt)) || !outcomes.has(run.build) ||
        (run.test != null && !outcomes.has(run.test))) {
      throw new Error('Actions 결과의 앱, 버전, 실행 ID, 시간 또는 상태가 올바르지 않습니다.');
    }
    const key = run.appId + ':' + run.runId;
    if (seen.has(key)) throw new Error('중복된 Actions 실행이 있습니다.');
    seen.add(key);

    const url = repositoryUrl + '/actions/runs/' + run.runId;
    const artifacts = {};
    for (const kind of ['apk', 'aab', 'web', 'test']) {
      const id = run.artifacts?.[kind];
      if (id == null) continue;
      if (!isId(id) || ((kind === 'apk' || kind === 'aab') && apps.get(run.appId).platform !== 'android') ||
          (kind === 'web' && apps.get(run.appId).platform !== 'web')) {
        throw new Error('Actions Artifact ID와 앱 플랫폼을 확인하세요.');
      }
      artifacts[kind] = url + '/artifacts/' + id;
    }
    const history = runs.get(run.appId) ?? [];
    history.push({
      versionName: run.versionName,
      updatedAt: new Date(run.updatedAt).toISOString(),
      build: run.build,
      test: run.test ?? null,
      url,
      artifacts
    });
    runs.set(run.appId, history);
  }
  for (const history of runs.values()) {
    history.sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
  }
  return runs;
}

export function createDashboardData(registry, snapshot = { runs: [] }) {
  const apps = normalizeApps(registry);
  const runs = normalizeRuns(apps, snapshot);
  return {
    actionsUrl: repositoryUrl + '/actions',
    apps: [...apps.values()].map(app => ({ ...app, runs: (runs.get(app.appId) ?? []).slice(0, 10) }))
  };
}

async function build() {
  const args = process.argv.slice(2);
  if (args.length && (args.length !== 2 || args[0] !== '--results')) {
    throw new Error('사용법: node dashboard/build.mjs [--results FILE]');
  }
  const registry = await readJson(new URL('../apps/registry.json', root));
  const snapshot = args.length ? await readJson(args[1]) : undefined;
  const data = createDashboardData(registry, snapshot);
  const output = new URL('dist/', root);
  await mkdir(output, { recursive: true });
  await copyFile(new URL('index.html', root), new URL('index.html', output));
  await copyFile(new URL('dashboard.css', root), new URL('dashboard.css', output));
  await copyFile(new URL('dashboard.mjs', root), new URL('dashboard.mjs', output));
  await writeFile(new URL('data.json', output), JSON.stringify(data, null, 2) + '\n');
  console.log(`대시보드 빌드 완료 · 앱 ${data.apps.length}개 · ${fileURLToPath(output)}`);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    await build();
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}

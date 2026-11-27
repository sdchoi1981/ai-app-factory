import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { appendFileSync, cpSync, existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { basename, join } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../', import.meta.url));
const generated = new Set(['.git', '.gradle', 'build', 'dist', 'local.properties', 'node_modules']);

test('Factory CLI scaffolds, validates, tests, builds and selects apps', t => {
  const sandbox = mkdtempSync(join(tmpdir(), 'factory-cli-'));
  t.after(() => rmSync(sandbox, { recursive: true, force: true }));

  for (const path of [
    'scripts', 'templates', 'packages', 'apps/registry.json',
    'apps/unitkit/settings.gradle', 'apps/unitkit/app/build.gradle',
    'apps/unitkit/test-conversions.js', 'apps/unitkit/app/src/main/assets/index.html'
  ]) {
    cpSync(join(root, path), join(sandbox, path), {
      recursive: true,
      filter: path => !generated.has(basename(path))
    });
  }

  const read = path => readFileSync(join(sandbox, path), 'utf8');
  const write = (path, contents) => writeFileSync(join(sandbox, path), contents);
  const exists = path => existsSync(join(sandbox, path));
  const run = (command, args, errorPattern) => {
    const result = spawnSync(command, args, { cwd: sandbox, encoding: 'utf8', timeout: 30_000 });
    assert.ifError(result.error);
    if (errorPattern) {
      assert.notEqual(result.status, 0, args.join(' '));
      assert.match(result.stderr, errorPattern);
    } else {
      assert.equal(result.status, 0, result.stderr);
    }
    return result.stdout.trim();
  };
  const cli = (script, args, errorPattern) => run(process.execPath, [`scripts/${script}.mjs`, ...args], errorPattern);
  const create = (id, platform, name, applicationId, errorPattern) => cli('create-app',
    [id, '--platform', platform, '--name', name, '--application-id', applicationId], errorPattern);
  const matrix = (...args) => JSON.parse(cli('validate-apps', ['--matrix', ...args])).include;
  const matrixIds = (...args) => matrix(...args).map(app => app.appId).sort();

  assert.deepEqual(matrixIds('--platform', 'android'), ['unitkit']);
  assert.match(cli('test-app', ['unitkit']), /tests passed/);
  create('smoke-android', 'android', "오늘's & Android", 'dev.factory.androidsmoke');
  create('smoke-web', 'web', '한글 & Web __APP_ID__', 'dev.factory.websmoke');

  const registryPath = 'apps/registry.json';
  const registry = read(registryPath);
  const entries = JSON.parse(registry).apps;
  for (const id of ['smoke-android', 'smoke-web']) {
    const app = entries.find(entry => entry.appId === id);
    assert.deepEqual(app.supportedLanguages, ['ko', 'en', 'ja']);
    assert.deepEqual(app.monetization, { ads: false, analytics: false });
    assert.equal(app.releaseConfiguration.enabled, false);
    assert.deepEqual(app.privacy.dataCollected, []);
    assert.equal(app.privacyPolicyUrl, null);
    assert.equal(app.buildConfiguration.path, `apps/${id}`);
    assert.equal(app.versionName, '1.0.0');
    assert.equal(app.versionCode, 1);
  }
  assert.deepEqual(matrixIds(), ['smoke-android', 'smoke-web', 'unitkit']);
  assert.deepEqual(matrixIds('--platform', 'web'), ['smoke-web']);
  assert.deepEqual(matrixIds('--platform', 'android', '--app', 'smoke-android'), ['smoke-android']);
  cli('validate-apps', ['--matrix', '--platform', 'web', '--app', 'unitkit'], /unknown app/i);
  cli('test-app', ['../registry'], /valid appId/i);

  const androidStrings = 'apps/smoke-android/app/src/main/res/values/strings.xml';
  assert.match(read(androidStrings), /오늘\\'s &amp; Android/);
  assert.match(read('apps/smoke-web/src/index.html'), /<title>한글 &amp; Web __APP_ID__<\/title>/);
  cli('test-app', ['smoke-android']);
  cli('test-app', ['smoke-web']);

  const existing = 'apps/existing';
  mkdirSync(join(sandbox, existing));
  write(`${existing}/keep.txt`, 'existing file');
  const webSource = read('apps/smoke-web/src/index.html');
  create('../escaped', 'web', 'Invalid', 'dev.factory.invalid', /invalid appId/i);
  create('invalid-package', 'android', 'Invalid', 'invalid-package', /invalid applicationId/i);
  create('smoke-web', 'web', 'Replacement', 'dev.factory.replacement', /duplicate appId/i);
  create('second-web', 'web', 'Duplicate', 'dev.factory.websmoke', /duplicate applicationId/i);
  create('existing', 'web', 'Occupied', 'dev.factory.occupied', /EEXIST/);
  assert.equal(read(registryPath), registry);
  assert.equal(read('apps/smoke-web/src/index.html'), webSource);
  assert.equal(read(`${existing}/keep.txt`), 'existing file');
  assert.equal(exists('escaped'), false);
  assert.equal(exists('apps/invalid-package'), false);
  assert.equal(exists('apps/second-web'), false);
  assert.equal(exists('apps/.registry.lock'), false);

  const invalidRegistry = JSON.parse(registry);
  invalidRegistry.apps.find(app => app.appId === 'smoke-web').buildConfiguration.path = '../escaped';
  write(registryPath, JSON.stringify(invalidRegistry));
  cli('validate-apps', ['--matrix'], /invalid buildConfiguration/i);
  write(registryPath, registry);

  cli('build-app', ['smoke-web']);
  const webOutput = 'apps/smoke-web/dist';
  assert.equal(read(`${webOutput}/index.html`), webSource);
  assert.match(read(`${webOutput}/robots.txt`), /Disallow: \//);
  assert.equal(exists(`${webOutput}/sitemap.xml`), false);
  for (const file of ['core/index.mjs', 'ui/index.mjs', 'ui/styles.css', 'localization/index.mjs']) {
    assert.equal(read(`${webOutput}/packages/${file}`), read(`packages/${file}`));
  }
  write('apps/smoke-web/src/.env', 'SYNTHETIC_TEST=not-a-secret\n');
  cli('build-app', ['smoke-web'], /unexpected file in public build input/i);
  assert.equal(exists(`${webOutput}/.env`), false);
  assert.equal(read(`${webOutput}/index.html`), webSource);
  rmSync(join(sandbox, 'apps/smoke-web/src/.env'));

  const git = (...args) => run('git', args);
  git('init', '--quiet', '--template=');
  git('config', 'user.name', 'Factory smoke test');
  git('config', 'user.email', 'factory-test@example.invalid');
  git('config', 'commit.gpgsign', 'false');
  git('add', 'apps/registry.json', 'apps/smoke-web/src', 'packages');
  git('commit', '--quiet', '-m', 'Scaffold baseline');
  const base = git('rev-parse', 'HEAD');
  assert.deepEqual(matrixIds('--base', base), []);

  appendFileSync(join(sandbox, 'packages/localization/index.mjs'), '\n// Test change\n');
  git('add', 'packages/localization/index.mjs');
  git('commit', '--quiet', '-m', 'Change web localization');
  assert.deepEqual(matrixIds('--base', base), ['smoke-web']);
  assert.deepEqual(matrixIds('--base', base, '--platform', 'android'), []);

  const webOnly = git('rev-parse', 'HEAD');
  appendFileSync(join(sandbox, 'packages/core/index.mjs'), '\n// Test change\n');
  git('add', 'packages/core/index.mjs');
  git('commit', '--quiet', '-m', 'Change shared core');
  assert.deepEqual(matrixIds('--base', webOnly), ['smoke-android', 'smoke-web']);
  assert.deepEqual(matrixIds('--base', webOnly, '--app', 'unitkit'), []);
});

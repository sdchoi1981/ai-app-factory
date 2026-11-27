#!/usr/bin/env node
import { spawnSync } from 'node:child_process';
import { constants } from 'node:fs';
import { access, copyFile, mkdtemp, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { appDirectory, appIdPattern, getApp, root } from './registry.mjs';
const fail = message => { throw new Error(message); };
const tracks = ['internal', 'closed', 'production'];
const signing = ['ANDROID_KEYSTORE_PATH', 'ANDROID_STORE_PASSWORD', 'ANDROID_KEY_ALIAS', 'ANDROID_KEY_PASSWORD'];

function build(appId) {
  const env = { ...process.env };
  for (const name of [...signing, 'GOOGLE_APPLICATION_CREDENTIALS', 'SUPPLY_JSON_KEY', 'CLOUDFLARE_API_TOKEN', 'CLOUDFLARE_ACCOUNT_ID']) {
    delete env[name];
  }
  run(process.execPath, ['scripts/build-app.mjs', appId], env);
}

function parseArgs([appId, ...args]) {
  if (!appIdPattern.test(appId ?? '')) {
    fail('Usage: node scripts/deploy-app.mjs APP_ID [--apply] [--track internal|closed|production] [--production-approved] [--rollout 0.1] [--environment preview|production]');
  }
  const options = { appId };
  for (let i = 0; i < args.length; i++) {
    const key = args[i];
    if (Object.hasOwn(options, key)) fail('Duplicate deployment option.');
    if (['--apply', '--production-approved'].includes(key)) options[key] = true;
    else if (['--track', '--rollout', '--environment'].includes(key) && args[i + 1] && !args[i + 1].startsWith('--')) options[key] = args[++i];
    else fail('Unknown or incomplete deployment option.');
  }
  return options;
}

function run(command, args, env = process.env) {
  const result = spawnSync(command, args, { cwd: root, env, stdio: 'inherit' });
  if (result.error || result.status !== 0) fail(`${command} failed with exit status ${result.status ?? 'unavailable'}.`);
}

function requireEnv(names) {
  for (const name of names) {
    if (!process.env[name]) fail(`Missing deployment setting ${name}.`);
  }
}

async function requiredFile(path, message) {
  try {
    await access(path, constants.R_OK);
  } catch {
    fail(message);
  }
}

async function publishAndroid(app, path, track, rollout, approved) {
  requireEnv(signing);
  await requiredFile(process.env.ANDROID_KEYSTORE_PATH, 'Android keystore file is unreadable.');
  const credentials = process.env.GOOGLE_APPLICATION_CREDENTIALS || process.env.SUPPLY_JSON_KEY;
  if (!credentials) fail('Configure Play credentials with Workload Identity Federation or an ephemeral service account JSON file.');
  await requiredFile(credentials, 'Play credential file is unreadable.');
  const metadata = join(path, 'store/metadata');
  if (app.releaseConfiguration.publishMetadata === true) {
    await requiredFile(join(metadata, 'en-US/title.txt'), 'Generate and review English store metadata first.');
  }

  build(app.appId);
  const temporary = await mkdtemp(join(tmpdir(), 'factory-play-'));
  try {
    const bundle = join(temporary, 'app-release.aab');
    await copyFile(join(path, 'app/build/outputs/bundle/release/app-release.aab'), bundle);
    const jarsigner = process.env.JAVA_HOME ? join(process.env.JAVA_HOME, 'bin/jarsigner') : 'jarsigner';
    run(jarsigner, ['-keystore', process.env.ANDROID_KEYSTORE_PATH, '-storepass:env', 'ANDROID_STORE_PASSWORD', '-keypass:env', 'ANDROID_KEY_PASSWORD', bundle, process.env.ANDROID_KEY_ALIAS]);
    run(jarsigner, ['-verify', bundle]);
    run('bundle', ['exec', 'fastlane', 'android', 'publish'], {
      ...process.env,
      BUNDLE_GEMFILE: join(root, 'fastlane/Gemfile'),
      FASTLANE_SKIP_UPDATE_CHECK: '1',
      FASTLANE_OPT_OUT_USAGE: '1',
      SUPPLY_JSON_KEY: resolve(credentials),
      FACTORY_ANDROID_APPLICATION_ID: app.applicationId,
      FACTORY_ANDROID_VERSION_CODE: String(app.versionCode),
      FACTORY_ANDROID_TRACK: track,
      FACTORY_ANDROID_OWNER_APPROVED: String(approved),
      FACTORY_ANDROID_CLOSED_TRACK: app.releaseConfiguration.closedTrack ?? 'beta',
      FACTORY_ANDROID_ROLLOUT: rollout ?? '',
      FACTORY_ANDROID_BUNDLE: bundle,
      FACTORY_ANDROID_METADATA: metadata,
      FACTORY_ANDROID_PUBLISH_METADATA: String(app.releaseConfiguration.publishMetadata === true)
    });
  } finally {
    await rm(temporary, { recursive: true, force: true });
  }
}

async function publishWeb(app, path, environment) {
  if (app.releaseConfiguration.provider !== 'cloudflare') fail('Only the cloudflare provider supports direct upload.');
  const project = app.releaseConfiguration.projectName;
  if (!/^[a-z][a-z0-9-]{0,63}$/.test(project ?? '')) fail('Configure a valid existing Cloudflare Pages projectName.');
  requireEnv(['CLOUDFLARE_API_TOKEN', 'CLOUDFLARE_ACCOUNT_ID']);
  build(app.appId);
  const production = app.releaseConfiguration.productionBranch;
  if (!/^[A-Za-z0-9][A-Za-z0-9._/-]{0,79}$/.test(production ?? '') || production === 'factory-preview') {
    fail('Configure the existing Pages production branch, distinct from factory-preview.');
  }
  const account = process.env.CLOUDFLARE_ACCOUNT_ID;
  if (!/^[a-fA-F0-9]{32}$/.test(account)) fail('Invalid Cloudflare account ID.');
  const response = await fetch(`https://api.cloudflare.com/client/v4/accounts/${account}/pages/projects/${project}`, {
    redirect: 'error',
    headers: { Authorization: `Bearer ${process.env.CLOUDFLARE_API_TOKEN}` },
    signal: AbortSignal.timeout(20000)
  });
  if (!response.ok) fail(`Could not verify the Cloudflare Pages project (HTTP ${response.status}).`);
  const details = await response.json();
  if (!details.success || details.result?.production_branch !== production) {
    fail('The configured production branch differs from the Cloudflare Pages project.');
  }
  const branch = environment === 'production' ? production : 'factory-preview';
  run('npx', ['--yes', 'wrangler@4', 'pages', 'deploy', join(path, 'dist'), '--project-name', project, '--branch', branch], {
    ...process.env,
    WRANGLER_SEND_METRICS: 'false'
  });
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  const app = getApp(options.appId);
  const release = app.releaseConfiguration;
  const appPath = appDirectory(app);
  const apply = options['--apply'] === true;
  if (apply) {
    if (release.enabled !== true) fail('Deployment is disabled for this app.');
    if (!app.privacyPolicyUrl) fail('Configure the reviewed, public privacy policy HTTPS URL before publishing.');
  }
  if (app.platform === 'android') {
    if (options['--environment']) fail('Android deployment uses --track.');
    const track = options['--track'] ?? release.defaultTrack ?? 'internal';
    if (!tracks.includes(track)) fail('Unsupported Android deployment track.');
    const rollout = options['--rollout'];
    if (rollout && (track !== 'production' || !/^0\.\d+$/.test(rollout) || !(Number(rollout) > 0 && Number(rollout) < 1))) {
      fail('Rollout must be a fraction between zero and one and requires the production track.');
    }
    console.log(JSON.stringify({ appId: app.appId, platform: app.platform, track, versionCode: app.versionCode, rollout: rollout ?? null, apply, enabled: release.enabled === true }));
    if (!apply) return;
    const closed = release.closedTrack ?? 'beta';
    if (!/^[A-Za-z0-9:_-]{1,80}$/.test(closed) || /(?:^|:)production$|(?:^|:)internal$/i.test(closed)) {
      fail('Invalid Play closed testing track.');
    }
    const effectiveTrack = track === 'closed' ? closed : track;
    const approved = options['--production-approved'] === true;
    if (effectiveTrack === 'production' && !approved) fail('Production requires explicit owner approval.');
    await publishAndroid(app, appPath, effectiveTrack, rollout, approved);
    return;
  }
  if (app.platform !== 'web' || options['--track'] || options['--rollout']) fail('Web deployment uses --environment preview|production.');
  const environment = options['--environment'] ?? 'preview';
  if (!['preview', 'production'].includes(environment)) fail('Unsupported web deployment environment.');
  console.log(JSON.stringify({ appId: app.appId, platform: app.platform, environment, provider: release.provider, apply, enabled: release.enabled === true }));
  if (!apply) return;
  if (environment === 'production' && !options['--production-approved']) fail('Production requires explicit owner approval.');
  await publishWeb(app, appPath, environment);
}

main().catch(error => {
  console.error(error.message);
  process.exitCode = 1;
});

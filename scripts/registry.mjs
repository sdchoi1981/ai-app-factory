import { execFileSync } from 'node:child_process';
import { lstatSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

export const root = fileURLToPath(new URL('../', import.meta.url));
export const registryPath = join(root, 'apps/registry.json');
export const packageNames = ['core', 'ui', 'ads', 'analytics', 'localization', 'privacy'];
export const appIdPattern = /^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$/;
export const applicationIdPattern = /^[a-z][a-z0-9_]*(?:\.[a-z][a-z0-9_]*)+$/;
const appFilePattern = /^[a-zA-Z0-9_-]+(?:\/[a-zA-Z0-9_-]+)*\.[a-zA-Z0-9]+$/;
const javaKeywords = new Set(
  'abstract assert boolean break byte case catch char class const continue default do double else enum extends final finally float for goto if implements import instanceof int interface long native new package private protected public return short static strictfp super switch synchronized this throw throws transient try void volatile while true false null'.split(' ')
);
const record = value => value && typeof value === 'object' && !Array.isArray(value);
const text = value => typeof value === 'string' && value.trim() !== '';
const match = (value, pattern) => typeof value === 'string' && pattern.test(value);
const unique = values => new Set(values).size === values.length;

export function httpsUrl(value) {
  try {
    const url = new URL(value);
    return url.protocol === 'https:' && !url.username && !url.password;
  } catch {
    return false;
  }
}

export function validateRegistry(document, { checkFiles = true } = {}) {
  if (!record(document) || !Array.isArray(document.apps)) throw Error('registry must contain an apps array');
  const ids = new Set();
  const packageIds = new Set();
  for (const app of document.apps) {
    const require = (condition, detail) => {
      if (!condition) throw Error(`Registry entry ${app?.appId ?? '(unknown)'}: ${detail}`);
    };
    require(record(app) && match(app.appId, appIdPattern) && app.appId.length <= 40 && app.appId !== 'all', 'invalid appId');
    require(!ids.has(app.appId), 'duplicate appId');
    ids.add(app.appId);
    require(text(app.appName) && app.appName.length <= 80 && !/[\u0000-\u001f\u007f":<>|*?\\/]/.test(app.appName), 'invalid appName');
    require(text(app.description) && text(app.category), 'description and category are required');
    require(['android', 'web'].includes(app.platform), 'invalid platform');
    require(match(app.applicationId, applicationIdPattern) &&
      app.applicationId.split('.').every(segment => !javaKeywords.has(segment)), 'invalid applicationId');
    require(!packageIds.has(app.applicationId), 'duplicate applicationId');
    packageIds.add(app.applicationId);
    require(match(app.versionName, /^\d+(?:\.\d+){1,3}(?:-[A-Za-z0-9]+(?:[.-][A-Za-z0-9]+)*)?$/), 'invalid versionName');
    require(Number.isInteger(app.versionCode) && app.versionCode > 0 && app.versionCode <= 2100000000, 'invalid versionCode');
    require(['development', 'testing', 'released'].includes(app.status), 'invalid status');
    const languages = app.supportedLanguages;
    require(Array.isArray(languages) && languages.length && unique(languages) &&
      languages.every(value => match(value, /^[a-z]{2,3}(?:-[A-Za-z0-9]{2,8})*$/)), 'invalid supportedLanguages');
    const money = app.monetization;
    require(record(money) && typeof money.ads === 'boolean' && typeof money.analytics === 'boolean', 'invalid monetization');
    const build = app.buildConfiguration;
    require(record(build) && build.path === `apps/${app.appId}` && match(build.testFile, appFilePattern), 'invalid buildConfiguration');
    require(Array.isArray(build.sharedPackages) && unique(build.sharedPackages) &&
      build.sharedPackages.every(name => packageNames.includes(name)), 'invalid sharedPackages');
    const release = app.releaseConfiguration;
    require(record(release) && typeof release.enabled === 'boolean', 'invalid releaseConfiguration');
    require(app.platform !== 'android' || ['internal', 'closed', 'production'].includes(release.defaultTrack), 'invalid Android track');
    require(app.platform !== 'web' || ['static', 'cloudflare'].includes(release.provider), 'invalid web provider');
    require(app.privacyPolicyUrl === null || (text(app.privacyPolicyUrl) && httpsUrl(app.privacyPolicyUrl)), 'invalid privacyPolicyUrl');
    require(record(app.privacy) && Array.isArray(app.privacy.dataCollected) &&
      app.privacy.dataCollected.every(text) && text(app.privacy.notes), 'invalid privacy inventory');
    if (app.siteUrl !== undefined) {
      require(app.platform === 'web' && httpsUrl(app.siteUrl) && new URL(app.siteUrl).origin === app.siteUrl, 'siteUrl must be an HTTPS origin');
    }
    if (checkFiles) checkAppFiles(app, require);
  }
  return document;
}

export function appDirectory(app) {
  return join(root, app.buildConfiguration.path);
}

function checkAppFiles(app, require) {
  const directory = appDirectory(app);
  const checked = (relative, type) => {
    let current = directory;
    require(lstatSync(current, { throwIfNoEntry: false })?.isDirectory(), 'missing or unsafe app directory');
    for (const part of relative.split('/')) {
      current = join(current, part);
      const info = lstatSync(current, { throwIfNoEntry: false });
      require(info && !info.isSymbolicLink(), `missing or unsafe file ${relative}`);
    }
    require(lstatSync(current)[type](), `invalid file type for ${relative}`);
    return current;
  };
  checked(app.buildConfiguration.testFile, 'isFile');
  if (app.platform === 'web') {
    checked('src/index.html', 'isFile');
    return;
  }
  checked('settings.gradle', 'isFile');
  const gradle = readFileSync(checked('app/build.gradle', 'isFile'), 'utf8');
  const literals = { applicationId: app.applicationId, versionName: app.versionName };
  for (const [name, value] of Object.entries(literals)) {
    const match = gradle.match(new RegExp(`^\\s*${name}\\s*(?:=\\s*)?['"]([^'"]+)['"]\\s*$`, 'm'));
    require(match?.[1] === value, `${name} differs from app/build.gradle`);
  }
  const code = gradle.match(/^\s*versionCode\s*(?:=\s*)?(\d+)\s*$/m);
  require(Number(code?.[1]) === app.versionCode, 'versionCode differs from app/build.gradle');
}

export function readRegistry() {
  if (!lstatSync(join(root, 'apps')).isDirectory() || !lstatSync(registryPath).isFile()) {
    throw Error('Registry or apps directory has an unsafe file type');
  }
  return validateRegistry(JSON.parse(readFileSync(registryPath, 'utf8')));
}

export function getApp(appId) {
  if (!match(appId, appIdPattern)) throw Error('Pass a valid appId');
  const app = readRegistry().apps.find(entry => entry.appId === appId);
  if (!app) throw Error(`Unknown app: ${appId}`);
  return app;
}

export function git(args) {
  return execFileSync('git', args, { cwd: root, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 });
}

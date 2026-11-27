import { lstatSync, mkdirSync, readdirSync, readFileSync, renameSync, rmSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { parseArgs } from 'node:util';
import { appDirectory, readRegistry, registryPath, root, validateRegistry } from './registry.mjs';

const { values, positionals } = parseArgs({
  allowPositionals: true,
  options: {
    platform: { type: 'string' },
    name: { type: 'string' },
    'application-id': { type: 'string' },
    description: { type: 'string' },
    category: { type: 'string' }
  }
});
const [appId] = positionals;
const { platform, name } = values;
if (positionals.length !== 1 || !name || !platform || !values['application-id']) {
  throw Error('Usage: node scripts/create-app.mjs APP_ID --platform android|web --name NAME --application-id PACKAGE_ID [--description TEXT] [--category CATEGORY]');
}

const app = {
  appId,
  appName: name,
  description: values.description ?? `${name} development scaffold.`,
  platform,
  applicationId: values['application-id'],
  versionName: '1.0.0',
  versionCode: 1,
  category: values.category ?? 'tools',
  status: 'development',
  supportedLanguages: ['ko', 'en', 'ja'],
  monetization: { ads: false, analytics: false },
  buildConfiguration: {
    path: `apps/${appId}`,
    testFile: 'test.mjs',
    sharedPackages: platform === 'android' ? ['core', 'ui'] : ['core', 'ui', 'localization']
  },
  releaseConfiguration: platform === 'android'
    ? { enabled: false, defaultTrack: 'internal' }
    : { enabled: false, provider: 'static' },
  privacyPolicyUrl: null,
  privacy: { dataCollected: [], notes: 'Scaffold has no data collection. Review when implementing app features.' }
};
validateRegistry({ apps: [app] }, { checkFiles: false });

const html = value => value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
const substitutions = {
  __APP_ID__: appId,
  __APP_NAME__: html(name),
  __APP_NAME_XML__: html(name.replace(/[\\'"]/g, character => '\\' + character)),
  __APPLICATION_ID__: app.applicationId
};

function renderTemplate(source, destination) {
  for (const entry of readdirSync(source)) {
    const from = join(source, entry);
    const to = join(destination, entry);
    const type = lstatSync(from);
    if (type.isDirectory()) {
      mkdirSync(to);
      renderTemplate(from, to);
    } else if (type.isFile()) {
      const content = readFileSync(from, 'utf8').replace(
        /__[A-Z_]+__/g, token => Object.hasOwn(substitutions, token) ? substitutions[token] : token
      );
      writeFileSync(to, content, { flag: 'wx' });
    } else {
      throw Error(`Unexpected template entry: ${entry}`);
    }
  }
}

const appsDirectory = join(root, 'apps');
if (!lstatSync(appsDirectory).isDirectory()) throw Error('Unsafe apps directory');
const lock = join(appsDirectory, '.registry.lock');
try {
  mkdirSync(lock);
} catch (error) {
  if (error.code === 'EEXIST') throw Error('Another app creation is in progress. Check apps/.registry.lock if it was interrupted.');
  throw error;
}
const directory = appDirectory(app);
const pending = join(lock, 'registry.json');
let created = false;
let registered = false;
try {
  const document = readRegistry();
  document.apps.push(app);
  validateRegistry(document, { checkFiles: false });
  mkdirSync(directory);
  created = true;
  const templates = join(root, 'templates');
  const template = join(templates, platform);
  if (!lstatSync(templates).isDirectory() || !lstatSync(template).isDirectory()) {
    throw Error('Unsafe template directory');
  }
  renderTemplate(template, directory);
  validateRegistry(document);
  writeFileSync(pending, JSON.stringify(document, null, 2) + '\n', { flag: 'wx' });
  renameSync(pending, registryPath);
  registered = true;
  console.log(`Created ${appId}: ${app.buildConfiguration.path}`);
} finally {
  if (created && !registered) rmSync(directory, { recursive: true, force: true });
  rmSync(lock, { recursive: true, force: true });
}

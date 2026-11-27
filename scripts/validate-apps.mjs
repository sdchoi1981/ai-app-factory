import { isDeepStrictEqual, parseArgs } from 'node:util';
import { git, readRegistry } from './registry.mjs';

const { values } = parseArgs({
  options: {
    matrix: { type: 'boolean' },
    platform: { type: 'string' },
    app: { type: 'string' },
    base: { type: 'string' }
  }
});

const { apps } = readRegistry();
if (values.platform && !['android', 'web'].includes(values.platform)) throw Error('Invalid platform');
if (values.app && values.app !== 'all' && !apps.some(app => app.appId === values.app &&
  (!values.platform || app.platform === values.platform))) throw Error('Unknown app for selected platform');

let changed = null;
let previous = new Map();
if (values.base) {
  if (!/^[0-9a-f]{40}$/.test(values.base)) throw Error('Use a full Git base SHA');
  if (!/^0+$/.test(values.base)) {
    git(['cat-file', '-e', `${values.base}^{commit}`]);
    changed = git(['diff', '--no-renames', '--name-only', '-z', values.base, 'HEAD', '--'])
      .split('\0').filter(Boolean);
    if (changed.includes('apps/registry.json')) {
      const exists = git(['ls-tree', '--name-only', values.base, '--', 'apps/registry.json']).trim();
      if (exists) {
        const old = JSON.parse(git(['show', `${values.base}:apps/registry.json`]));
        previous = new Map(old.apps.map(app => [app.appId, app]));
      }
    }
  }
}

const affected = app => !changed || changed.some(file => {
  if (file === 'apps/registry.json') return !isDeepStrictEqual(previous.get(app.appId), app);
  if (file === '.github/workflows/build-android.yml') return app.platform === 'android';
  if (file === '.github/workflows/build-web.yml') return app.platform === 'web';
  return file.startsWith(`${app.buildConfiguration.path}/`) ||
    file.startsWith('scripts/') ||
    file === '.github/workflows/test-apps.yml' ||
    file === '.github/workflows/validate-apps.yml' ||
    app.buildConfiguration.sharedPackages.some(name => file.startsWith(`packages/${name}/`));
});

const selected = apps.filter(app => (!values.platform || app.platform === values.platform) &&
  (!values.app || values.app === 'all' || app.appId === values.app) && affected(app));
if (values.matrix) {
  console.log(JSON.stringify({ include: selected.map(({ appId, appName, versionName, versionCode, platform, buildConfiguration }) =>
    ({ appId, appName, versionName, versionCode, platform, path: buildConfiguration.path })) }));
} else {
  console.log(`Validated ${apps.length} registry entries. Selected: ${selected.map(app => app.appId).join(', ') || 'none'}`);
}

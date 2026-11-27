import { execFileSync } from 'node:child_process';
import { cpSync, lstatSync, mkdirSync, mkdtempSync, readdirSync, renameSync, rmSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { appDirectory, getApp, root } from './registry.mjs';

const app = getApp(process.argv[2]);
const directory = appDirectory(app);

function copyPublic(source, destination) {
  const info = lstatSync(source);
  if (info.isDirectory()) {
    mkdirSync(destination, { recursive: true });
    for (const entry of readdirSync(source)) {
      if (entry.startsWith('.') || /\.(pem|jks|keystore|p12|key)$/i.test(entry)) {
        throw Error(`Unexpected file in public build input: ${entry}`);
      }
      copyPublic(join(source, entry), join(destination, entry));
    }
  } else if (info.isFile()) {
    cpSync(source, destination, { errorOnExist: true, force: false });
  } else {
    throw Error(`Invalid public build input: ${source}`);
  }
}

if (app.platform === 'android') {
  execFileSync('gradle', [
    '--no-daemon', '--max-workers=4', ':app:assembleDebug', ':app:lintDebug', ':app:bundleRelease'
  ], { cwd: directory, stdio: 'inherit' });
  console.log(`APK/AAB output: ${app.buildConfiguration.path}/app/build/outputs`);
} else {
  const staging = mkdtempSync(join(directory, '.dist-'));
  try {
    copyPublic(join(directory, 'src'), staging);
    if (app.buildConfiguration.sharedPackages.length) {
      if (!lstatSync(join(root, 'packages')).isDirectory()) throw Error('Unsafe packages directory');
      mkdirSync(join(staging, 'packages'));
      for (const name of app.buildConfiguration.sharedPackages) {
        const source = join(root, 'packages', name);
        if (!lstatSync(source).isDirectory()) throw Error(`Unsafe package root: ${name}`);
        const destination = join(staging, 'packages', name);
        mkdirSync(destination);
        for (const entry of readdirSync(source).filter(file => /^[a-z0-9-]+\.(mjs|css)$/.test(file))) {
          copyPublic(join(source, entry), join(destination, entry));
        }
      }
    }
    if (app.siteUrl) {
      writeFileSync(join(staging, 'sitemap.xml'),
        `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"><url><loc>${app.siteUrl}/</loc></url></urlset>\n`,
        { flag: 'wx' });
    }
    writeFileSync(join(staging, 'robots.txt'), app.siteUrl
      ? `User-agent: *\nAllow: /\nSitemap: ${app.siteUrl}/sitemap.xml\n`
      : 'User-agent: *\nDisallow: /\n', { flag: 'wx' });
    const output = join(directory, 'dist');
    rmSync(output, { recursive: true, force: true });
    renameSync(staging, output);
    console.log(`Static output: ${app.buildConfiguration.path}/dist`);
  } finally {
    rmSync(staging, { recursive: true, force: true });
  }
}

#!/usr/bin/env node
import { lstat, mkdir, readFile, readdir, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import { appDirectory, getApp } from './registry.mjs';

const locales = ['ko-KR', 'en-US', 'ja-JP'];
const fileOptions = ['--icon', '--feature-graphic', '--screenshots', '--screenshot-provenance'];
const fail = message => { throw new Error(message); };

function parseArgs([appId, ...args]) {
  if (!appId) fail('Usage: node scripts/generate-store-assets.mjs APP_ID [--icon PNG] [--feature-graphic PNG] [--screenshots DIR --screenshot-provenance FILE]');
  const options = {};
  for (let index = 0; index < args.length; index += 2) {
    const [key, value] = args.slice(index, index + 2);
    if (!fileOptions.includes(key) || Object.hasOwn(options, key) || !value || value.startsWith('--')) {
      fail('Unknown, duplicate, or incomplete store asset option.');
    }
    options[key] = value;
  }
  if (Boolean(options['--screenshots']) !== Boolean(options['--screenshot-provenance'])) {
    fail('Supply both the screenshot directory and provenance file.');
  }
  return { appId, options };
}

async function ensureDirectory(directory) {
  try {
    await mkdir(directory);
  } catch (error) {
    if (error.code !== 'EEXIST') throw error;
  }
  if (!(await lstat(directory)).isDirectory()) fail('Store asset output directory is unsafe.');
}

async function boundedRead(path, limit) {
  const info = await lstat(path);
  if (!info.isFile() || !info.size || info.size > limit) fail(`Invalid input file: ${path}`);
  return readFile(path);
}

async function writeGenerated(directory, segments, content) {
  for (const part of segments.slice(0, -1)) {
    directory = join(directory, part);
    await ensureDirectory(directory);
  }
  const path = join(directory, segments.at(-1));
  try {
    await writeFile(path, content, { flag: 'wx' });
  } catch (error) {
    if (error.code !== 'EEXIST') throw error;
    const expected = Buffer.isBuffer(content) ? content : Buffer.from(content);
    if (!(await boundedRead(path, 8 * 1024 * 1024)).equals(expected)) {
      fail(`Existing store asset differs: ${path}. Review and archive it before regenerating.`);
    }
  }
}

function listingText(value, limit, label) {
  if (typeof value !== 'string' || !value.trim() ||
      Array.from(value.trim()).length > limit ||
      /[\u0000-\u0008\u000b\u000c\u000e-\u001f\u007f]/.test(value) ||
      (limit <= 80 && /[\r\n]/.test(value))) {
    fail(`Missing or invalid ${label} (limit: ${limit} characters).`);
  }
  return value.trim() + '\n';
}

async function image(path, kind) {
  const limit = kind === 'icon' ? 1024 * 1024 : 8 * 1024 * 1024;
  const png = await boundedRead(path, limit);
  if (png.length < 33 || png.subarray(0, 8).toString('hex') !== '89504e470d0a1a0a' || png.toString('ascii', 12, 16) !== 'IHDR') {
    fail(`Expected a PNG asset: ${path}`);
  }
  const width = png.readUInt32BE(16);
  const height = png.readUInt32BE(20);
  const color = png[25];
  if (png[24] !== 8 || (kind === 'icon' ? color !== 6 : color !== 2)) {
    fail(`Expected an 8-bit ${kind === 'icon' ? 'RGBA' : 'RGB'} PNG: ${path}`);
  }
  if (kind === 'icon' && (width !== 512 || height !== 512)) fail('Icon must be 512 × 512.');
  if (kind === 'feature' && (width !== 1024 || height !== 500)) fail('Feature graphic must be 1024 × 500.');
  if (kind === 'screenshot') {
    const min = Math.min(width, height);
    const max = Math.max(width, height);
    if (min < 320 || max > 3840 || max > 2 * min) fail('Screenshot dimensions do not meet Play constraints.');
  }
  return png;
}

async function main() {
  const { appId, options } = parseArgs(process.argv.slice(2));
  const app = getApp(appId);
  if (app.platform !== 'android') fail('Play store asset preparation is supported for Android apps.');
  const store = join(appDirectory(app), 'store');
  await ensureDirectory(store);
  const source = JSON.parse(await boundedRead(join(store, 'listing.json'), 64 * 1024));
  const generated = [];
  for (const locale of locales) {
    const listing = source[locale];
    if (!listing) fail(`Missing reviewed store copy for ${locale}.`);
    const metadata = ['metadata', locale];
    generated.push(
      [[...metadata, 'title.txt'], listingText(listing.title, 30, `${locale} title`)],
      [[...metadata, 'short_description.txt'], listingText(listing.shortDescription, 80, `${locale} short description`)],
      [[...metadata, 'full_description.txt'], listingText(listing.fullDescription, 4000, `${locale} description`)],
      [[...metadata, 'changelogs', `${app.versionCode}.txt`], listingText(listing.releaseNotes, 500, `${locale} release notes`)]
    );
    if (options['--icon']) generated.push([[...metadata, 'images', 'icon.png'], await image(options['--icon'], 'icon')]);
    if (options['--feature-graphic']) generated.push([[...metadata, 'images', 'featureGraphic.png'], await image(options['--feature-graphic'], 'feature')]);
  }
  if (options['--screenshots']) {
    if (!(await lstat(options['--screenshots'])).isDirectory()) fail('Expected screenshot directories by Play locale.');
    const provenance = await boundedRead(options['--screenshot-provenance'], 16 * 1024);
    let count = 0;
    for (const locale of locales) {
      const directory = join(options['--screenshots'], locale);
      const info = await lstat(directory, { throwIfNoEntry: false });
      if (!info) continue;
      if (!info.isDirectory()) fail(`Unsafe screenshot directory: ${locale}`);
      const names = (await readdir(directory)).sort();
      if (names.length < 2 || names.length > 8 || names.some(name => !/^[a-zA-Z0-9_-]+\.png$/.test(name))) {
        fail(`Provide two to eight ordered PNG screenshots in ${locale}.`);
      }
      for (const [index, name] of names.entries()) {
        const png = await image(join(directory, name), 'screenshot');
        generated.push([['metadata', locale, 'images', 'phoneScreenshots', `${index + 1}.png`], png]);
        count++;
      }
    }
    if (!count) fail('No supported locale screenshots to package.');
    generated.push([['screenshot-provenance.md'], provenance]);
  }
  const privacy = app.privacy.dataCollected.length
    ? app.privacy.dataCollected.map(value => `- ${value}`).join('\n')
    : '- 현재 등록된 수집 항목 없음';
  generated.push(
    [['privacy-policy-draft.md'], `# ${app.appName} 개인정보처리방침 초안\n\n` +
      `기기 밖으로 전송되거나 수집되는 것으로 등록된 데이터\n\n${privacy}\n\n${app.privacy.notes}\n\n` +
      `공개 정책 URL: ${app.privacyPolicyUrl ?? '미설정'}\n\n` +
      '배포 전에 운영자 연락처, 보관 및 삭제 정책, 아동 대상 여부, 적용 지역의 의무와 최신 SDK 동작을 검토하세요.\n'],
    [['data-safety-review.md'], `# ${app.appName} Google Play 데이터 보안 검토\n\n` +
      `버전 ${app.versionName} (${app.versionCode})\n\n기록된 수집 항목\n\n${privacy}\n\n` +
      `광고 설정: ${app.monetization.ads ? '켜짐' : '꺼짐'}\n통계 설정: ${app.monetization.analytics ? '켜짐' : '꺼짐'}\n\n` +
      '제출 전 매니페스트, 외부 SDK, 네트워크 동작 및 실제 릴리스 설정을 대조하세요. 이 문서는 Play Console 신고를 자동 제출하지 않습니다.\n']
  );
  for (const [segments, content] of generated) await writeGenerated(store, segments, content);
  console.log(`Prepared ${generated.length} reviewed-input assets in ${app.buildConfiguration.path}/store.`);
}

main().catch(error => {
  console.error(error.message);
  process.exitCode = 1;
});

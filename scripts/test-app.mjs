import { spawnSync } from 'node:child_process';
import { appDirectory, getApp } from './registry.mjs';

const app = getApp(process.argv[2]);
const result = spawnSync(process.execPath, [app.buildConfiguration.testFile], {
  cwd: appDirectory(app), stdio: 'inherit'
});
if (result.error) throw result.error;
process.exitCode = result.status ?? 1;

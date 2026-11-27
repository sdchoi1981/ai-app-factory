# AI App Factory operations

## Start a task

Use the existing checkout of `sdchoi1981/ai-app-factory`, the requested baseline, and a dedicated branch for your changes. Read [AGENTS.md](../AGENTS.md) and the instructions for the selected app. UnitKit lives under `apps/unitkit`. GitHub Actions is the authoritative source of CI and downloadable artifact status.

Start the work from an owner prompt in a host session with repository access. The [orchestrator instructions](../agents/orchestrator.md) describe how to delegate to actual host subagents or work through the same roles in sequence. File-based role instructions are usable in an interactive session. Unattended task triggers and scheduling require a separately configured service.

A useful prompt identifies the app, the outcome, constraints and required evidence:

> Improve UnitKit's one-handed use, preserve the current conversions, verify accessibility and offline operation, open a PR and report the build and downloadable artifacts. Keep signing and publication out of scope.

## Run the work

1. Record the task scope and acceptance criteria with the planner and designer. Identify required tests, artifacts and permissions early.
2. Implement on the task branch. Perform reviewer, security and tester passes with the integrated change. Keep each role's evidence in the task record and attribute subagent results only when the role used an actual subagent.
3. Run the app's targeted checks from the repository root. Replace `unitkit` with the selected app ID for other apps.

   ```bash
   node scripts/test-app.mjs unitkit
   node scripts/build-app.mjs unitkit
   ```

4. Open or update the authorized PR. Record the PR URL and head SHA. Find the corresponding Actions run, inspect the required jobs and wait for their terminal conclusions. If access is blocked, record the denied operation and preserve the local work.
5. When a required test or CI job has an actionable failure, record its first meaningful error, fix the cause, and rerun the failing check plus affected checks. Use at most three repair attempts for the task. Count targeted corrections and deliberate reruns as attempts. Polling an existing run does not count as an attempt. Stop the repair loop early when a remaining failure depends on missing permissions, infrastructure or credentials.
6. On CI success, verify that the relevant artifacts exist in that run. Report the app ID, PR URL, commit, run and artifact links, the local versus CI results, and outstanding checks. If jobs are skipped or artifacts are absent, report those outcomes explicitly.

## Keep task evidence

Use the task's working notes or PR description for the evidence record. Include the app ID, branch, current head SHA, acceptance criteria, role assignments and results, exact commands with outcomes, and PR/Actions/artifact URLs. For each repair enter the attempt number, failing check, diagnosis, change or retry, and resulting check status. Keep secret values, personal data and unrelated project details out of this record.

| Workflow | Purpose |
| --- | --- |
| `validate-apps.yml` | Validate app manifests and layout |
| `test-apps.yml` | Run app tests |
| `build-android.yml` | Build Android development artifacts |
| `build-web.yml` | Build static web artifacts |
| `release-android.yml` | Explicit Android release preparation |
| `deploy-web.yml` | Explicit web deployment workflow |

From a GitHub-authenticated checkout, use the PR head to locate the matching runs and inspect their outputs:

```bash
head_sha=$(gh pr view "$pr_number" --json headRefOid --jq .headRefOid)
gh run list --commit "$head_sha" --event pull_request
gh run view "$run_id"
gh run download "$run_id" --dir "$download_dir"
```

Set `pr_number` to the selected PR, `run_id` to the matching run, and `download_dir` to the intended local artifact directory. UnitKit local outputs are under `apps/unitkit/app/build/outputs/`. Static web builds write `apps/<app-id>/dist/`. A successful registry job may select zero apps for a platform. Check the matrix and artifact list before reporting a build.

A local debug APK is for testing. An unsigned release AAB requires authorized signing before store submission. Keep store publication, production web deployment, release signing, domain purchases, paid services and live ad configuration within explicit owner authorization and the applicable workflow approval gates. Use only the authorized repository. Device smoke tests and store acceptance are separate from automated builds and lint.

## Historical UnitKit baseline

[Run 45940014820](https://github.com/sdchoi1981/ai-app-factory/actions/runs/45940014820) verified 94 conversions, Android lint and builds, APK signature verification, and artifact upload before the monorepo migration. Validate current changes on their own commit and look up their corresponding Actions artifacts.

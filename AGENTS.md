# AI App Factory operating rules

Build small, offline-first Android apps and static web utilities from owner prompts. Work only in the authorized repository and app scope. Read [AI operations](docs/AI-OPERATIONS.md) and any app-level instructions before editing.

## Roles and task execution

The lead follows [orchestrator](agents/orchestrator.md) and owns integration and the final report. Follow these role instructions for each task:

| Role | Responsibilities |
| --- | --- |
| [Planner](agents/planner.md) | Scope, app selection, acceptance criteria and required checks |
| [Designer](agents/designer.md) | User flow, responsive layout, accessibility and localization |
| [Developer](agents/developer.md) | Implement scoped changes and run local checks |
| [Reviewer](agents/reviewer.md) | Independently inspect behavior, maintainability and scope |
| [Security](agents/security.md) | Review privacy, permissions, dependencies and release risks |
| [Tester](agents/tester.md) | Exercise acceptance criteria, boundaries and regressions |
| [Release manager](agents/release-manager.md) | Verify PR, CI, artifacts and release authorization |

Use actual host-provided subagents when the current task and host permit delegation. Give each agent the app ID, acceptance criteria, read/write scope and evidence to return. The lead integrates their work and resolves findings. If subagents are unavailable, conduct and report explicit sequential passes with the same outputs. Markdown role files guide the current session. Scheduled or unattended execution requires a separately configured host capability.

## Repository workflow

- Use the existing cloud checkout and a dedicated branch based on the requested baseline. Create a worktree only if the user asks for one.
- Select the app by its app ID. Run `node scripts/test-app.mjs <app-id>` and `node scripts/build-app.mjs <app-id>` from the repository root as applicable. Use CI evidence for the exact PR head and verify artifacts before linking them.
- Record the scope, checks, results, current commit and remaining blockers. Use at most three repair attempts per task and explain unresolved failures.
- Prefer local computation, minimal permissions and no account requirement. Preserve accessibility, offline behavior and compatibility with existing users and stored preferences.
- Never commit credentials, keys, personal data or unrelated code. Keep Android security controls and Play Protect intact. Use debug APKs for testing and state whether any AAB is unsigned.
- Keep paid services, live ad IDs, domain purchases, release signing, store publication, and production deployments behind explicit owner authorization. Do not access unrelated repositories or financial projects. Report real-device and store acceptance separately from automated validation.

## Conditional auto-merge

- Open a PR targeting `main` for each completed task; never bypass the main ruleset.
- After the reviewer and security passes, enable GitHub auto-merge (prefer squash when supported) only for owner-requested, in-scope changes that need no further owner approval. Require the `Factory required checks` check to succeed for the current PR revision. Never merge failing or pending checks manually, or weaken the check to pass.
- If auto-merge is unavailable or permission is denied, report the PR URL and exact blocker. Do not claim a merge until GitHub confirms it. Verify main-branch CI after merging. Changes to protection rules, release workflows, secrets, live ads, paid services or production deployment require separate owner authorization.

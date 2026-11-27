# AI App Factory operations

## Verified baseline
UnitKit Android API 36 CI succeeded in run 45940014820; 94 conversion checks passed, Android lint/build and APK signature verification completed, debug APK and unsigned AAB were uploaded. This is not device or Play Store acceptance testing.

## Codex Cloud environment
Owner: connect only sdchoi1981/ai-app-factory and publish a reusable cloud environment via Codex on web/desktop. Use main as baseline. No secrets or paid API credentials required for current scope. Node 22 is sufficient for local conversion tests; GitHub Actions is the authoritative Android build environment.

## One-prompt workflow
1. Interpret request and AGENTS.md; use a dedicated branch for changes.
2. Implement and review; run node test-conversions.js.
3. Create a PR; inspect GitHub Actions results. If failed, inspect first actionable error, fix and rerun, at most three attempts.
4. Report PR, test outcomes and artifact links. Do not mark a build successful until Actions says success and artifact exists.
5. Request owner action only for permissions, account setup, paid services, protected merges, signing, publishing, or real-device installation when needed.

Example: "Improve UnitKit for one-handed use; keep all conversions working, review and test it, open a PR and verify APK generation."

## Limits
AGENTS.md and this runbook are guidance, not an always-on agent scheduler. Codex Cloud environment must be created in the owner's account. No production publishing, real ad IDs, automatic paid API, or release signing has been enabled.

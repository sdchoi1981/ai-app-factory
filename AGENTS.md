# AI App Factory — agent operating rules

Goal: deliver small useful offline-first apps and static websites from a short owner prompt.
Use the existing repository only; never access unrelated repositories.

## Workflow
1. PM: translate the prompt into acceptance criteria and scope; choose reasonable defaults.
2. Designer: plan responsive, accessible UI and localization.
3. Engineer: implement the smallest complete working solution.
4. Reviewer: independently review correctness, privacy, security, licenses, and store policy. Use separate subagents if available; otherwise perform explicit review passes.
5. QA: write and run tests, include invalid input and boundaries. Fix failures, rerun tests, and report unresolved issues.
6. Build: run the repository CI, inspect logs and artifacts. Never claim APK/AAB generated until the workflow actually succeeds.
7. Deliver: provide links, test results, installation steps, limitations and any necessary owner actions.

## Guardrails
- Do not commit secrets, signing keys, personal information, credentials or unrelated proprietary code.
- No paid services, live ad IDs, production publishing, domain purchases or release signing without owner approval.
- Do not weaken Android security or bypass Play Protect. Debug APKs are test-only; unsigned AABs are not store-ready.
- Prefer offline processing, no login, no unnecessary permissions. Add ads only after consent/privacy review and explicit owner request.
- Keep project changes scoped, testable and documented. Use branches/PRs for subsequent changes. Keep a bounded repair loop (at most three attempts per task), then report.
- These rules guide Codex; they do not themselves create or run autonomous agents.

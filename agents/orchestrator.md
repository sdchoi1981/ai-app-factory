# Orchestrator

- Identify the authorized repository, baseline, app ID, requested outcome and release limits. Protect existing changes and use a dedicated branch for code changes.
- Keep a task record with scope, acceptance criteria, role assignments and outputs, check results, current commit, PR and run links, artifacts, and a repair count. Use progressive statuses such as scoped, implementing, reviewing, verifying, complete, and blocked.
- Assign work via actual host subagent tools when delegation is allowed. Parallelize independent design, implementation and review work only when inputs and edit boundaries allow it. Ensure that independent reviewers can see the implementation and acceptance criteria. With no subagent capability, perform the same roles in sequence and record how they were executed.
- Collect the planner and designer outputs before settling implementation scope. Have the reviewer, security role and tester verify the integrated change before delivery. Assign PR and artifact evidence to the release manager.
- For an actionable failure attributable to the task, capture the command or CI job and first meaningful error, select a targeted correction, and rerun the failing and affected checks. Record each repair attempt and stop after the third, or earlier when further progress requires outside action. Keep the existing PR updated when one exists.
- Return the decision and evidence for each required check. Label failed, skipped, and unrun checks separately. Distinguish generated local outputs, verified artifacts on CI, and actual release/deployment.

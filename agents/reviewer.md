# Reviewer

- Compare the integrated diff to the acceptance criteria. Trace the normal entry points and check behavioral compatibility, shared interfaces, error handling, and maintainability.
- Look for regressions outside the changed code, including documentation that no longer matches commands and artifacts. Confirm the tests exercise the requested behavior.
- Return findings with severity, file locations, observable effect and a practical correction. Distinguish blocking findings from follow-up work. Verify the revised diff when a blocking finding is addressed.

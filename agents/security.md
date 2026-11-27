# Security

- Review permission and data flows from input through storage, network and outputs. Confirm offline constraints, credential handling, platform security settings, and absence of unintended telemetry.
- Check changed dependencies or bundled assets for relevant provenance and licensing. Check CI token scope, untrusted input handling, and whether publishing or signing is gated.
- For Android, examine exported components, app/site network access, WebView isolation where applicable, and debug versus release signing. For web, examine unsafe DOM writes, third-party assets and accidental publication of private files.
- Return concrete findings and mitigations with file locations. Review defensively and avoid building exploit payloads. Record the limits of static and automated review.

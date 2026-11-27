# Kit design system — phase 1

## Scope and design decisions
First production-quality direction for PassportKit and PhotoKit, preserving existing native Java processing. Native shared tokens are the first incremental implementation, not a completed Jetpack Compose/Material 3 migration. No new libraries or media assets. Other apps keep their existing screens.

- Colors: semantic background/surface/ink/muted/outline/accent/onAccent/tint, light and dark.
- Layout: 640dp content maximum, centered, 20dp outer margins; cards 22dp radius, controls 14dp, minimum 52dp height, native font scaling and scroll.
- App flow: choose source → edit/verify → export. Keep errors and status visible. Never imply official passport approval.
- PassportKit: country/source card, large crop stage, manual guide confirmation, separate output stage and actual paper layout preview. Keep JPEG-only input, original-only official modes, face hints, secure flag, splash and export gates.
- PhotoKit: actual bounded thumbnail, three tool presets, format/quality/size controls, indeterminate processing, actual output byte count. Batch ZIP remains intact; no fabricated savings or before/after comparison.
- Device QA: light/dark, large font, 320/360dp phone and 600+dp foldable, TalkBack, orientation, picker cancel, corrupt image, export cancel, manual guide validation, print scale and offline use.
- Delivery gates: app tests, Android lint, APK/AAB and exact head merge check. Screenshot automation, Compose migration, and actual device visual review remain follow-ups.

Roles: sequential planner/designer/developer/reviewer/security/tester/release passes; no independent subagent or local Android SDK available in this host. Static wiring tests supplement, not replace, CI and device testing.

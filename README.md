# AI App Factory

Prompt-driven development of offline-first Android apps and static web utilities. The initial project is UnitKit (Android 16, API 36). The root Android project is a starting template, not a fully autonomous orchestration service.

## Build
Use JDK 17, Gradle 8.13 and Android SDK platform 36. Run node test-conversions.js and gradle :app:assembleDebug :app:lintDebug :app:bundleRelease.
In GitHub Actions, use Build UnitKit Android to generate debug APK and unsigned release bundle when CI is configured. Download artifacts after a successful run.

## Codex
Connect this repository in Codex Cloud. Follow AGENTS.md. Example: "Improve UnitKit accessibility and usability, run tests, create a PR, and report failures and artifact links." Cloud setup, merge, and production release remain owner-controlled. No automatic API-powered repair daemon is included.

## Safety
This repository is public. Never store credentials or keys here. The app has no ads or internet permission. Debug APKs are test-only; unsigned AABs require release signing. Keep Play Protect enabled; investigate security warnings.

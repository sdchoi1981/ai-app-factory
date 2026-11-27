# First AAB signing (separate from Play upload)

No production keys are generated or stored by this repository. Google Play App Signing and owner-managed upload keys are recommended. Use a different app-specific upload key, securely backed up, before first registration. Never send keys or passwords in chat or commit them.

## Manual workflow
The sign-only workflow is disabled unless the owner creates the repository variable FACTORY_ENABLE_AAB_SIGNING=true, selects main, chooses one registered Android app, and explicitly confirms owner-approved. Create an environment named signing-APP_ID (for example signing-unitkit), restrict it to main, and configure the four environment secrets ANDROID_KEYSTORE_BASE64, ANDROID_STORE_PASSWORD, ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD. Configure required reviewers if available. Never use repository-wide signing secrets. Build and tests run in a separate job with no keys. The signing job downloads the unsigned bundle and signs and verifies it. It never contacts Play.

**Public repository warning:** GitHub Actions artifacts may be accessible to other GitHub users. The signed AAB does not contain the private signing key, but can reveal unreleased code. Do not enable this workflow unless you accept public artifact visibility; use a private signing repository or local signing otherwise. Artifacts expire after one day. Store the approved signed bundle securely. No credentials, private keys or key files are uploaded.

## Local alternative
Build using node scripts/build-app.mjs APP_ID. Set the four environment variables ANDROID_KEYSTORE_PATH, ANDROID_KEY_ALIAS, ANDROID_STORE_PASSWORD, ANDROID_KEY_PASSWORD locally without exposing their values. Run node scripts/sign-aab.mjs APP_ID /absolute/path/outside/repository/APP_ID-release.aab. The script rejects pre-signed input and verifies the resulting signature. It cannot verify Play registration or upload-key acceptance; that is a separate owner action.

Actual signing, release upload, ads and analytics remain disabled until the owner authorizes them and configures the required credentials.

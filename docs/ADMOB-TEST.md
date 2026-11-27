# AdMob test banners

Ten existing Android apps share BannerActivity and the Gradle configuration in packages/ads/android. Ordinary builds use a no-op Activity superclass: no ads SDK, no ad permissions added, no production IDs. Run Gradle from a supported app with -PfactoryTestAds=true :app:assembleDebug for a separate .adtest package. Release variants are disabled in this mode. The test manifest removes AD_ID and uses Google's sample app ID and anchored adaptive test banner ID. A separate CI workflow uploads TEST-ADS-APK artifacts. New apps require the same opt-in wiring before use.

The test activity refreshes UMP consent, provides privacy options when required, and does not initialize ads unless consent refresh/form completed successfully and canRequestAds is true. Sample app ID may not provide a configured UMP form. If consent fails, no ad is shown; do not bypass consent to force a test banner. Device testing is required for UMP, banner visibility, keyboard/insets, orientation and lifecycle. Advertising SDK communication occurs only in test builds; production app privacy disclosures must be reviewed separately.

No production ad support, live ID configuration or revenue reporting is enabled. Before production, register each app with AdMob, review audience/consent/privacy and data safety, verify app-ads.txt, and approve a distinct live configuration change. Use banner only, no interstitial or rewarded placements yet. Dashboard reports source configuration, not proof of ad delivery.

References: https://developers.google.com/admob/android/quick-start ; https://developers.google.com/admob/android/banner ; https://developers.google.com/admob/android/privacy

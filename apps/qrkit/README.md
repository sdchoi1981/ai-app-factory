# QRKit

Offline QR generator and camera scanner.

Android 8.0+, target API 36. Offline, English, Korean and Japanese. No ads or analytics. Device smoke testing and Play registration remain pending. Build via factory scripts.

Uses ZXing core 3.5.4 and JourneyApps embedded 4.3.0 (Apache 2.0). Camera is invoked only by user action, scan content stays local and is not auto-opened. Physical camera behavior requires device verification. See https://github.com/zxing/zxing and https://github.com/journeyapps/zxing-android-embedded for licenses and upstream sources.

## 1.0.1 camera hotfix

User reported app exit on camera button. Replaced default CaptureActivity integration with internal scanner screen, explicit runtime camera permission, camera error result, and lifecycle-aware preview. No device crash log was provided; root cause not confirmed. Retest permission allow/deny, preview, actual QR decode, cancel/back, rotation, fold/unfold and busy camera on Android device.

## Create and Scan workspaces

Create shows a high-contrast preview, byte count and explicit generation action. Scan starts the camera only on request, explains the camera state and separates the result from camera controls. Results can be selected or copied by the user. The app does not keep a history of QR contents.

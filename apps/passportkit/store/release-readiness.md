# PassportKit Google Play preparation

Baseline app `com.factory.passportkit`, 1.5.3 (10), Android 8+, target API 36. References [#22](https://github.com/sdchoi1981/ai-app-factory/issues/22), [#23](https://github.com/sdchoi1981/ai-app-factory/issues/23), and [#29](https://github.com/sdchoi1981/ai-app-factory/issues/29).

## Listing draft

| Field | English | Korean |
| --- | --- | --- |
| Name | PassportKit | PassportKit 여권사진 |
| Short description | Crop passport photos offline and save JPEGs or print-ready PDFs. | 오프라인에서 여권사진을 자르고 JPEG 또는 인쇄용 PDF로 저장하세요. |

**English.** Import a JPEG or take a photo, then adjust the crop and head guides. Prepare Korean passport photos and US or Japanese print layouts on your device. Save JPEGs to your gallery or export a print sheet as a PDF. Check the issuing authority's requirements before applying.

**한국어.** JPEG를 가져오거나 사진을 찍고 자르기와 얼굴 위치 가이드를 조정하세요. 한국 여권사진과 미국·일본 여권사진 인쇄용 파일을 기기에서 만들 수 있습니다. JPEG는 갤러리에 저장하고, 인쇄용 시트는 PDF로 내보낼 수 있습니다. 신청 전에 발급 기관의 사진 요건을 확인하세요.

## Evidence before changing the dashboard status

- Record the PR head, required checks, Android lint, privacy-verified APK, debug APK and unsigned release AAB. Compare Gradle version and target API against the registry and the most recent Play upload.
- Run fresh-install capture checks with synthetic photos. Test guided and external camera separately with permission granted and denied, then verify JPEG import, crop, head guide, all three export paths and provider cancellation. Check Android 8–9 gallery permission and Android 10+ scoped storage.
- Review the Photo, Edit and Save screens in light and dark mode, on narrow and wider devices, with larger text and TalkBack. Verify text and navigation in English, Korean, Japanese, Simplified Chinese and Traditional Chinese. Keep the app's secure window enabled. Capture store evidence through an authorized method that preserves this protection and uses no personal photos.
- Verify current Google Play target API, account-specific testing and listing requirements in Play Console. Prepare listing images and icons, review the [privacy policy draft](privacy-policy-draft.md), and confirm Data safety answers against the final build, including bundled SDK behavior and user-selected providers. The registry currently lists no collected data and has no published privacy policy URL.
- With separate owner authorization, verify signing, an internal or closed testing installation, and available crash and ANR evidence. Record account and device results before updating the dashboard from testing. Ads and billing require separate test integration, consent and policy verification before production activation.

[Google Play testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465) · [Data safety](https://support.google.com/googleplay/android-developer/answer/10787469) · [App signing](https://developer.android.com/studio/publish/app-signing)

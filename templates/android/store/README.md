# Google Play 등록 자료

앱의 실제 기능과 출시 버전을 확인한 다음 `listing.example.json`을 `listing.json`으로 복사하고 빈 값을 채웁니다. 앱 화면에서 지원하는 언어와 스토어에 번역해 제공하는 언어를 구분합니다.

레지스트리의 개인정보 목록은 매니페스트, 의존 SDK 및 네트워크 동작과 대조합니다. 아이콘과 Feature Graphic은 확정된 앱 디자인에서 만들고, 스크린샷은 해당 릴리스 후보의 기기 화면에서 캡처합니다. 캡처 기기, 버전, 로캘을 기록합니다.

저장소 루트에서 `node scripts/generate-store-assets.mjs __APP_ID__`를 실행합니다. 이미지 입력 옵션은 [배포 가이드](../../../fastlane/README.md)에 있습니다. 실제 Play 등록 및 서명은 소유자 인증과 검토 이후 진행합니다.

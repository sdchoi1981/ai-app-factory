# UnitKit 최초 Google Play 등록 체크리스트

현재 기준: applicationId `com.unitkit.converter`, versionName 1.1, versionCode 2, 광고/분석 비활성, 공개 개인정보처리방침 URL 미설정. 등록 직전 실제 빌드와 대조한다.

## 1. 계정 소유자 작업
- Play Console의 신원, 연락처 및 실제 Android 기기 인증 상태를 확인한다.
- 무료 도구 앱 UnitKit을 생성한다. 패키지 ID는 첫 번들에서 확인하며 이미 다른 앱에서 사용 중이면 등록 전 고유 ID를 결정한다.
- 실제 Android 기기에서 기능, 화면, 접근성 및 오프라인 동작을 검증하고 실제 화면을 캡처한다.
- 개인정보처리방침 초안을 검토하고 운영자 연락처를 정한 뒤 공개 HTTPS URL에 게시한다. 데이터 보안, 콘텐츠 등급 등 정책 신고는 실제 출시 구성과 일치하도록 직접 확인한다.

## 2. 첫 AAB
- Google Play App Signing에서 Google 관리 앱 서명 키를 사용하는 방향을 권장한다. 업로드 키는 소유자가 안전한 환경에서 생성하고 백업한다.
- 업로드 키/비밀번호를 GitHub 소스, 공개 대시보드, 채팅 또는 CI Artifact에 넣지 않는다.
- 현재 CI의 unsigned-release-AAB는 그대로 제출할 수 없다. 소유자 승인 후 별도의 서명 전용 흐름으로 첫 업로드용 AAB를 만들고 검증한다. 기존 publish 작업은 Google Play API 인증과 업로드를 함께 요구하므로 첫 업로드 전에는 비활성 상태로 유지한다.
- Play Console 내부 테스트 트랙에 첫 서명 AAB를 업로드하고 Play에서 설치되는 실제 버전을 검증한다.

## 3. 후속 자동 배포
- 앱별 업로드 키를 보호된 GitHub Environment Secrets에 넣고, Google Cloud Play Developer API와 최소 권한 서비스 계정 및 Workload Identity Federation을 구성한다.
- 기존 release-android.yml과 Fastlane의 dry-run을 먼저 확인한다. 인증/서명/배포를 실제로 검증하기 전 활성화하지 않는다.
- 신규 개인 개발자 계정의 비공개 테스트 자격을 충족한다. Google Play의 정식 출시 권한 신청 및 승인과 별도 소유자 출시 승인을 거친다.
- 광고 또는 분석 SDK를 활성화하기 전에 개인정보처리방침, 동의 및 데이터 보안 신고를 다시 검토한다.

공식 참고: https://developer.android.com/studio/publish/app-signing ; https://support.google.com/googleplay/android-developer/answer/9842756 ; https://support.google.com/googleplay/android-developer/answer/9859348 ; https://support.google.com/googleplay/android-developer/answer/14151465

이 문서는 작업 순서이며 실제 서명 키 생성, 마켓 등록, 업로드, 정책 신고 및 출시를 수행하지 않는다.

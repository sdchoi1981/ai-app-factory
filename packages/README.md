# 공통 모듈

| 모듈 | 사용처 |
| --- | --- |
| core | 웹 저장소·설정·버전 관리와 Android 앱 전용 SharedPreferences |
| ui | 웹 CSS 컴포넌트·테마·대화상자와 Android 시스템 영역·주야간 테마 |
| localization | 한국어·영어·일본어 웹 번역과 언어 선택. Android 앱은 표준 values, values-ko, values-ja 리소스 사용 |
| privacy | 앱별 수집 내역 및 정책 URL의 형식과, Google UMP 브리지의 동의 수명 주기 |
| ads | 승인 설정, 동의와 노출 간격으로 제어하는 Android AdMob 어댑터 경계 |
| analytics | 사용·오류·수익 이벤트의 기본 비활성 어댑터 경계 및 목적별 승인 확인 |

Android 스캐폴드는 Gradle sourceSets로 공통 Java·리소스를 참조합니다. 웹 스캐폴드는 빌드가 정적 출력에 복사한 공유 CSS와 ES 모듈을 사용합니다.

AdMob SDK는 별도 동의 검토 뒤 각 Android 앱에 연결합니다. 브리지는 Startup에 Google UMP 동의 정보 업데이트 및 폼 처리를 연결하고 UMP의 광고 요청 가능 여부를 반환합니다. SDK 초기화와 요청은 허용 상태에서만 진행합니다. UMP가 요구하는 경우 개인정보 설정 메뉴를 연결하고, 동의 철회 때 기존 배너를 제거하고 로드된 전면·보상 광고를 폐기합니다. 동일 세션 중 배너 반복 노출 및 광고 기회는 앱 흐름에서도 제한합니다. 실서버 광고 운영을 승인받기 전에는 코드에 명시된 공식 Google Android 샘플 ID만 사용합니다. 보상은 SDK의 보상 콜백에서만 지급합니다.

이 경계에는 SDK, 인증값, 네트워크 요청이 포함되지 않습니다. 동의 UI만으로 법적 요구사항이 충족되지는 않습니다. 분석 SDK를 연결하기 전에는 실제 이벤트와 필드를 정책과 스토어 신고에 반영하고 앱에서 철회 경로를 제공합니다. 앱 입력, 식별자, 민감정보를 수집하지 않는 기본값을 유지합니다.

네이티브 Android 앱에는 `packages/privacy/android/java`의 `UmpAdapter`와 `packages/ads/android/java`의 `AdMobAdapter` 연결점이 있습니다. 연동 승인 후에만 해당 경로를 앱의 Gradle sourceSets에 추가하고 SDK를 연결합니다. 앱은 메인 스레드에서 동의 상태 변경을 반영하고, 동의 철회 시 `clear()`로 표시 및 대기 광고를 제거합니다. Java 인터페이스는 SDK를 불러오지 않습니다.

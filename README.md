# AI App Factory

Android 앱과 정적 웹 앱을 한 저장소에서 개발하고 검증합니다. 앱마다 디렉터리, 버전, 테스트, 빌드 및 배포 설정을 가집니다. 현재 등록된 제품은 오프라인 단위 변환 앱 UnitKit입니다.

| 경로 | 용도 |
| --- | --- |
| `apps/registry.json` | 앱 식별자, 버전, 의존 공통 모듈, 개인정보 및 배포 설정 |
| `apps/unitkit/` | UnitKit Android 프로젝트 |
| `packages/`, `templates/` | Android·웹 공통 모듈과 신규 앱 기본 구조 |
| `scripts/`, `.github/workflows/` | 생성, 검증, 앱별 테스트·빌드, 마켓 자료, 배포 |
| `agents/`, `docs/` | 자연어 요청 운영 절차 및 역할별 완료 기준 |
| `dashboard/`, `fastlane/` | 관리 대시보드와 Google Play 배포 구성 |

## 개발 시작

Node.js 22를 사용합니다. Android 빌드는 JDK 17, Gradle 8.13, Android SDK 플랫폼 36, Platform Tools 및 Build Tools 35.0.0과 36.0.0이 필요합니다. 기존 Codex 환경에서는 `source /workspace/.unitkit-tools/activate.sh`로 도구를 활성화합니다.

저장소 루트에서 실행합니다.

```sh
node scripts/validate-apps.mjs
node scripts/test-app.mjs unitkit
node scripts/build-app.mjs unitkit
```

APK와 서명되지 않은 AAB는 각각 `apps/unitkit/app/build/outputs/apk/debug/app-debug.apk`, `apps/unitkit/app/build/outputs/bundle/release/app-release.aab`에 생성됩니다. 기기 테스트에는 디버그 APK를 사용합니다.

## 새 앱 추가

```sh
node scripts/create-app.mjs habit-timer --platform android --name 'Habit Timer' --application-id org.example.habittimer
node scripts/create-app.mjs color-notes --platform web --name 'Color Notes' --application-id org.example.colornotes
node scripts/test-app.mjs color-notes
node scripts/build-app.mjs color-notes
```

예시 ID는 실제 운영 전에 소유한 이름으로 바꿉니다. 등록 후 Android의 패키지 ID와 버전은 Gradle 설정과 함께 관리합니다. 웹 빌드는 `apps/<appId>/dist/`에 생성됩니다. 검색 노출이 필요한 공개 웹 앱에는 `siteUrl`로 검증된 HTTPS 출처를 등록합니다. 빌드에서 해당 출처를 사용해 sitemap과 robots.txt를 생성합니다. 미설정 빌드는 크롤링을 허용하지 않습니다.

## 대시보드

```sh
node dashboard/build.mjs
python3 -m http.server 4173 --bind 127.0.0.1 --directory dashboard/dist
```

브라우저에서 포트 4173을 엽니다. Actions 이력과 산출물은 실제 실행을 확인한 스냅샷을 입력하면 표시됩니다. 연동되지 않은 광고·다운로드·오류 통계는 미연결로 표시합니다. [대시보드 입력 형식](dashboard/README.md)

## 운영 및 배포

자연어 개발 요청은 Codex에서 시작합니다. 요청을 역할별로 처리하고 앱별 테스트, 빌드, 리뷰와 산출물을 기록합니다. [중앙 지침](AGENTS.md) · [AI 작업 운영 절차](docs/AI-OPERATIONS.md)

일반 CI는 변경된 앱과 공통 모듈의 영향을 받는 앱을 검증합니다. 릴리스 워크플로는 보호된 브랜치에서 수동 실행하며, 앱 등록과 호스팅 설정에서 모두 활성화해야 실제로 게시합니다. 서명 키와 인증 정보는 저장소 밖에서 관리합니다. [배포 설정](fastlane/README.md)

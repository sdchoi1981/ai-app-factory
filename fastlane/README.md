# 배포 준비

배포 계획은 네트워크나 비밀정보 없이 확인할 수 있습니다.

```bash
node scripts/deploy-app.mjs unitkit
node scripts/deploy-app.mjs unitkit --track closed
node scripts/deploy-app.mjs unitkit --track production --rollout 0.1
```

## Google Play

1. 담당자가 Play Console에서 앱을 만들고 초기 앱 설정, 최초 업로드, 테스트 자격, 개인정보처리방침, 앱 콘텐츠 및 데이터 보안 신고에 필요한 사항을 완료합니다.
2. 등록 정보의 패키지 ID, 버전 코드, 공개 HTTPS 개인정보처리방침 URL을 확인합니다. 등록 자료 생성기는 번역 검토를 마친 직접 작성 자료와 실제 화면 캡처를 입력으로 받습니다. 앱에 없는 기능은 문구에 넣지 마세요.
3. Google Cloud에서 Play Developer API를 활성화하고 필요한 앱과 트랙에만 릴리스 권한을 부여합니다. GitHub Actions에는 제한적인 Workload Identity Federation과 서비스 계정 임퍼소네이션을 우선 사용합니다. 환경별로 저장소와 브랜치 조건을 걸고, 정식 출시 환경에 승인자를 등록합니다. 서비스 계정 JSON이 필요한 경우 GitHub Environment 비밀값에 저장하고 작업 중에만 임시 파일로 사용합니다.
4. 각 환경의 업로드 키와 비밀번호는 GitHub Environment 비밀값으로 관리합니다. 로컬에서 승인된 배포를 실행할 때는 `ANDROID_KEYSTORE_PATH`, `ANDROID_STORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` 및 `GOOGLE_APPLICATION_CREDENTIALS` 또는 `SUPPLY_JSON_KEY`를 설정합니다. 마지막 두 변수는 JSON 본문 대신 파일 경로를 받습니다.
5. CI의 수동 Android 릴리스 워크플로를 이용합니다. 앱의 `releaseConfiguration.enabled`와 환경 배포 설정이 기본으로 꺼져 있습니다. 승인 후 앱 설정을 켜고 내부 테스트부터 배포합니다. `closedTrack`을 생략하면 `beta`를 사용합니다. 정식 출시는 보호된 환경의 사용자 승인과 명시적인 production 확인을 모두 요구합니다. `--rollout 0.1`은 Play가 이미 단계적 배포를 허용하는 업데이트에만 적용합니다.

승인된 배포 명령은 앱을 빌드하고 임시 App Bundle을 JDK로 서명하고 검증합니다. Fastlane은 Play에서 조회 가능한 모든 트랙의 최대 버전 코드를 확인하고 그보다 높은 번들만 업로드합니다. 출시에 이전에 사용한 버전 코드는 Play 자체에서도 거부됩니다. `publishMetadata`가 명시적으로 켜진 앱만 검토된 `store/metadata`를 업로드합니다. 인증 경로와 실제 트랙 업로드는 인증이 제공된 보호된 환경에서 검증해야 합니다.

## Cloudflare Pages

계정 생성, 과금 플랜 선택 및 도메인 구매는 사용자가 담당합니다. 기존 Pages 프로젝트, 공개 HTTPS 개인정보처리방침, 앱별 `releaseConfiguration`의 `provider: "cloudflare"`, `projectName`, `productionBranch`를 구성합니다. 실제 배포를 하려면 `enabled`를 켜고 Cloudflare Pages 범위로 제한한 `CLOUDFLARE_API_TOKEN`과 `CLOUDFLARE_ACCOUNT_ID`를 보호된 환경에 추가합니다. 프리뷰 브랜치는 `factory-preview`이며 스크립트가 Cloudflare에서 확인한 운영 브랜치와 다를 때만 배포합니다. 운영 배포는 사용자 승인 이후에 진행합니다.

정적 호스팅에서는 `node scripts/build-app.mjs APP_ID` 결과인 `apps/APP_ID/dist`를 승인된 호스팅에 올릴 수 있습니다. 앱의 `siteUrl`과 운영 도메인을 일치시키고 Search Console에서 도메인 소유권을 확인한 뒤 사이트맵을 제출하세요. AdSense 승인, 정책 검토, 동의 구현 이후에만 운영 광고를 연결합니다.

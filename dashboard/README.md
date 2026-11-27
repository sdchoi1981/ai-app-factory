# AI App Factory 대시보드

저장소 루트에서 대시보드를 빌드하고 정적 서버로 확인합니다.

```sh
node dashboard/build.mjs
python3 -m http.server 3100 --bind 127.0.0.1 --directory dashboard/dist
```

`dashboard/dist`를 정적 호스팅에 올릴 수 있습니다. 앱 목록, 버전 및 개발 상태는 `apps/registry.json`에서 읽습니다. 마켓, 광고 수익, 다운로드 통계, 오류 통계는 연결이 없는 상태로 표시합니다. 결과 파일에는 공개할 수 있는 정보만 넣으세요.

## GitHub Actions 결과 가져오기

Actions의 앱별 실행 결과와 Artifact ID를 확인한 후 스냅샷을 저장합니다. 앱의 워크플로 실행에서 확인한 정보로만 작성합니다. 스냅샷이 없으면 빌드 및 테스트 상태에 미연결이 표시됩니다. 스냅샷을 다시 제공해 빌드할 때 데이터가 갱신됩니다.

```sh
node dashboard/build.mjs --results /path/to/verified-actions-results.json
```

스냅샷은 `runs` 배열을 포함하는 JSON 객체입니다. 각 항목에 `appId`, `versionName`, 양의 정수 `runId`, UTC ISO 8601 `updatedAt`, `build` 상태를 기록합니다. 선택 항목으로 `test` 상태와 `artifacts`를 넣을 수 있습니다. 상태 값은 `queued`, `in_progress`, `success`, `failure`, `cancelled`, `skipped`입니다. `artifacts`에는 `apk`, `aab`, `web`, `test` 키에 확인된 양의 정수 Artifact ID를 넣습니다.

대시보드는 등록된 앱별 최근 10개 실행을 표시하며 같은 저장소의 Actions 주소를 실행 ID와 Artifact ID로 구성합니다. Artifact 만료와 다운로드 권한은 GitHub 설정을 따릅니다.

## GitHub Pages deployment

Owner: Settings > Pages > Build and deployment > Source: GitHub Actions. This public, read-only dashboard publishes only registry metadata and verified main-branch job outcomes and unexpired artifact links. No Play credentials, signing keys, or revenue data are included. After main merge, Publish factory dashboard runs on relevant main changes and completed main builds/tests, or manually. GitHub authentication may still be needed for artifact downloads. No Cloudflare or paid hosting required. This is not a private admin console.

# UnitKit Play 등록 자료

`listing.json`에는 한국어, 영어, 일본어 스토어 설명과 출시 노트가 있습니다. 일본어 설명은 앱 화면에서 한국어와 영어를 사용할 수 있다고 명시합니다. 아이콘과 배너는 앱의 벡터 아이콘을 사용해 만들었습니다. 개인정보처리방침과 데이터 보안 문서는 등록 전 검토용입니다.

현재 Play용 스크린샷은 없습니다. 기기나 에뮬레이터에서 이 앱의 릴리스 후보를 열고 화면을 캡처하세요. 예를 들어 USB 디버깅이 연결된 기기에서 `adb exec-out screencap -p > 01.png`를 실행할 수 있습니다. 개인정보가 포함되지 않도록 확인하고, 기기/OS, 앱 버전, 언어 및 화면 캡처 절차를 출처 문서에 기록하세요. 지원되는 Play 로캘별 하위 디렉터리(예를 들어 `en-US`)에 두 장에서 여덟 장의 8비트 RGB PNG를 넣습니다. 이미지는 최소 변 320px, 최대 변 3840px, 비율 2:1 이내여야 합니다.

```bash
node scripts/generate-store-assets.mjs unitkit \
  --screenshots /path/to/screenshots --screenshot-provenance /path/to/capture.md
```

리소스가 달라지면 기존 산출물을 검토하고 백업한 뒤 교체하세요. 생성기는 동일한 파일로 반복 실행할 수 있습니다. 리뷰와 Play Console에서의 최종 등록은 담당자가 수행합니다.

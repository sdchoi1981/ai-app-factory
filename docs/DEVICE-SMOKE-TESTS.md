# Device smoke tests: ten Android apps

Install each current debug APK from the factory dashboard; verify Android installation security normally. No production signing or Play upload is needed. Record app version, device model, Android version, result and screenshots with synthetic data only.

| App | Minimum smoke test |
| --- | --- |
| UnitKit | 1 km = 1000 m; switch category; decimal input; rotate |
| DateKit | 2024-02-28 to 2024-03-01 gives 2 days, inclusive 3; change date; D-day |
| CalcKit | 12000 split by 3 = 4000; divide by zero rejected |
| QRKit | Generate synthetic text, scan on a second screen, deny then grant camera; no URL auto-open |
| LoanKit | 1200 at 0% over 12 months = 100 per month; invalid rate rejected |
| DiscountKit | 100 with 20% then 25% = 60 before tax |
| TimeKit | 2026-01-01 00:00 UTC, Seoul 09:00, New York prior day 19:00 |
| JsonKit | Pretty print and minify synthetic JSON; malformed input rejected |
| TextKit | Unicode, multiline, copy result |
| RandomKit | Pick 6 unique numbers from 1–45; invalid range rejected |

For every app: check Korean/English, large text, dark mode, rotation, offline operation, back navigation, keyboard and screen insets. Capture real device screens for Play listings. Automated CI is not evidence that these device tests passed.

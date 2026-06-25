# scripts/agent — 에이전트 표준 명령어

AI 에이전트가 명령을 추측하지 않도록 한 표준 진입점. 모두 POSIX `sh`/`bash`.
**Windows**: Git Bash 또는 `bash scripts/agent/<name>` 으로 실행.

| 스크립트 | 역할 | destructive |
|----------|------|:-----------:|
| `bootstrap` | 환경 점검 + 시크릿 파일 안내 | 아니오 |
| `run-local` | 로컬(H2) 앱 실행 | 아니오 |
| `test` | 테스트 (`--tests` 인자 전달 가능) | 아니오 |
| `build` | 클린 빌드(테스트 포함) | 아니오 |
| `verify` | 빌드+테스트+ArchUnit+시크릿/문서 점검 (= CI build 게이트) | 아니오 |
| `lint` | Spotless 포맷 점검 (soft, 비차단) | 아니오 |
| `check-docs` | 마크다운 깨진 링크 + generated 신선도 | 아니오 |
| `generate-project-map` | `docs/generated/project-map.md` 재생성 | 덮어씀(생성물만) |
| `generate-api-index` | `docs/generated/api-index.md` 재생성 | 덮어씀(생성물만) |
| `generate-data-schema` | `docs/generated/data-schema.md` 재생성 | 덮어씀(생성물만) |

## 원칙

- 실패 시 원인 + 다음 행동을 함께 출력한다.
- 시크릿은 예시 파일(`application-secret.yml.example`)만 둔다. 실제 시크릿은 커밋 금지.
- 운영에 영향을 주는 명령(배포/운영 SSH)은 이 디렉터리에 두지 않는다 — 배포는 `.github/workflows/` 전담.
- `generate-*` 는 `docs/generated/` 의 자동 생성물만 덮어쓴다(다른 파일 건드리지 않음).

PR 전 권장 순서: `scripts/agent/verify` → (필요 시) `scripts/agent/generate-*` → 문서 갱신.

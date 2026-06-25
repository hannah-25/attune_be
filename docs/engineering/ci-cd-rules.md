# CI/CD 규칙

## 워크플로 (현재)

| 파일 | 트리거 | 역할 |
|------|--------|------|
| `.github/workflows/ci.yml` | PR + push(비배포 브랜치) | **검증 게이트**: 빌드 + 테스트 + 아키텍처 규칙 + 문서 점검 |
| `.github/workflows/deploy-dev.yml` | push `develop` | dev 빌드(`-x test`) → Docker → EC2 배포 + health 대기 |
| `.github/workflows/deploy-prod.yml` | (운영 배포) | prod 배포 |

> 배포 워크플로는 하네스 도입 시 **변경하지 않았다**. 검증은 신규 `ci.yml` 이 담당한다.

## 게이트 정책

- PR은 `ci.yml` 통과를 머지 조건으로 삼는다. (required check 강제 여부는 GitHub 브랜치 보호 설정에서 관리)
- `ci.yml` 의 `build` 잡은 **테스트를 포함**해 실행한다(배포 빌드의 `-x test` 와 다름).
- 자동 포매터(Spotless 등)는 현재 사용하지 않는다. 스타일은 리뷰로 관리한다.

## 시크릿

- 배포 시크릿은 GitHub Secrets(`APPLICATION_SECRET_YML`, `DOCKER_*`, `DEV_EC2_*`)로 관리. 워크플로 외부에 노출 금지.
- CI 검증 잡은 시크릿 없이 H2/스텁으로 동작해야 한다.

## 로컬에서 CI 재현

```bash
scripts/agent/verify   # CI build 잡과 동일한 검증(빌드+테스트+아키텍처)
```

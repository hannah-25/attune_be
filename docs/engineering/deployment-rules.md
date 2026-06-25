# 배포 규칙

## 흐름

```
push develop ─> GitHub Actions(deploy-dev) ─> gradle build(-x test)
   ─> Docker build/push (hannah098/attune-be:dev)
   ─> EC2 SSH: docker pull/run (host network, SPRING_PROFILES_ACTIVE=dev)
   ─> /actuator/health 200 대기(최대 600s)
```

운영(prod)은 `deploy-prod.yml`. 동일 패턴.

## 규칙

1. 배포는 **워크플로를 통해서만**. 에이전트가 운영 SSH/배포를 임의 실행하지 않는다.
2. 배포 빌드는 테스트를 스킵(`-x test`)하므로, **검증 책임은 `ci.yml`** 에 있다. 테스트 깨진 채 develop 머지 금지.
3. dev 배포는 `APP_MIGRATION_DEFAULTTAGS_ENABLED=false` 환경변수로 마이그레이션 토글을 끈 채 기동한다(중복 시드 방지).
4. 헬스체크가 600초 내 200이 아니면 배포 실패로 간주하고 컨테이너 로그를 확인한다.
5. 롤백: 직전 이미지 태그로 `docker run` 하거나, 문제 커밋을 revert 후 재배포.

## 배포 전 확인

- `ci.yml` green
- 새 환경변수/시크릿이 필요하면 GitHub Secrets·EC2 환경에 반영되었는지 확인
- DB 스키마 변경이 있으면 [data-rules](../architecture/data-rules.md) 절차 준수

장애 대응은 [`../quality/reliability.md`](../quality/reliability.md).

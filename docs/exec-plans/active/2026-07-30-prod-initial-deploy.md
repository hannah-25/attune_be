# 실행 계획: 운영(prod) 최초 배포

- 상태: active
- 작성자 / 날짜: Claude / 2026-07-30
- 관련 이슈/PR: `chore/prod-release-prep` 브랜치 (PR 미생성)
- 선행 결과: [실사용 환경 검증 계획](2026-07-29-release-readiness-validation.md), [dev 100 VU 부하 테스트](2026-07-28-dev-load-test.md)

## 목표

`main` 브랜치와 운영 환경을 되살려 attune 백엔드를 prod 에 처음으로 배포한다.
완료 시점에 운영 서버가 `develop` 기준 코드로 기동하고, readiness 와 핵심 사용자 경로가 동작한다.

## 배경

- `main` 의 마지막 배포는 2026-05-24(`eb44fd7`)이고 이후 운영 배포가 없었다.
- 그 사이 `develop` 이 400 커밋 앞서 있다. 콘텐츠 diff 는 606 파일 / +36,021 / -2,311 이다.
- 즉 이번 작업은 일상적인 배포가 아니라 **사실상 최초 릴리스**에 가깝다.
- 실사용자와 실데이터가 없으므로 데이터 보존 제약이 없고, DB 를 새로 구축할 수 있다.

## 현재 상태

### 이미 준비된 것

| 항목 | 확인 내용 |
| --- | --- |
| 배포 워크플로 | `.github/workflows/deploy-prod.yml`. dev 와 동일 패턴(빌드 → Docker push → SSH → readiness 폴링). `main` push 또는 `workflow_dispatch` |
| 시크릿 저장 | GitHub Secrets. 리포지터리에 커밋되지 않는다. 환경별 분리는 GitHub Environments 로 전환했다([시크릿 노출](#시크릿-노출)) |
| 부하테스트 코드 격리 | `LoadtestDataRunner` 는 `@Profile("loadtest")` + `loadtest.data.action` 기본값 `none`. `deploy-prod.yml` 은 두 값 모두 넘기지 않아 이중 차단 |
| prod EC2 | 존재 |
| 관측 | prod 만 `SENTRY_ENABLED=true`, `APP_RELEASE=${{ github.sha }}` 주입 |

### 아직 없는 것

- prod DB (신규 생성 필요)
- prod Redis (신규 생성 필요)
- `main` ← `develop` 머지

### 조사 중 발견한 문제

1. **`app.frontend-url` 이 prod 에서 빈 값** — `application-prod.yml:62`. `AccountService:76,142` 가 이메일 인증·비밀번호 재설정 링크의 base URL 로 쓴다. 빈 값이면 기동과 readiness 는 정상인데 링크만 상대경로로 나가는 조용한 실패다. → 수정 완료
2. **마이그레이션 도구 부재** — Flyway/Liquibase 없음. `docs/sql/` 에 21개 SQL 이 수동 적용 전제로 쌓여 있다. prod 는 `ddl-auto: validate` 라 스키마 불일치 시 기동 자체가 실패한다(조용히 깨지지는 않음).
3. **`journal_tags` 가 시스템 태그와 유저 커스텀 태그를 한 테이블에 담음** — `scope` 로만 구분하고 `scope='USER'` 행은 `owner_user_id` 로 `users(id)` 를 참조한다. 통째로 덤프하면 유저가 없는 prod 에서 FK 제약으로 import 가 실패한다.
4. **`docs/db_schema.md` 드리프트** — `Hospital (병원)` 항목이 남아있으나 코드에 엔티티도 리포지터리도 없다. 문서만으로 테이블을 판단할 수 없다는 뜻이다.
5. **`APP_MIGRATION_DEFAULTTAGS_ENABLED` 는 죽은 환경변수** — `app.migration.*` 프로퍼티도 Migration 클래스도 코드에 없다. dev/prod 워크플로 양쪽에 남은 잔재이며 무해하다.
6. **롤백 대상이 없음** — `deployment-rules.md` 의 롤백은 "직전 이미지 태그로 재실행"인데 prod 는 첫 배포라 직전 이미지가 없다.

7. **시크릿이 public Docker 이미지에 구워지고 있었음** — 아래 [시크릿 노출](#시크릿-노출) 참고. → 수정 완료

8. **prod 로그가 배포마다 소실** — `application-prod.yml:27-32` 가 `/var/log/attune-me/app.log` 에 30일 보관으로 설정하지만 `docker run` 에 볼륨이 없었다. 배포마다 `docker rm` 이 돌아 보관 설정과 무관하게 전부 사라지고 장애 추적이 불가능하다. → 수정 완료

## 시크릿 노출

배포 워크플로가 `APPLICATION_SECRET_YML` 을 빌드 전에 `src/main/resources/` 에 쓰고
있었다. `build.gradle` 에 리소스 제외 규칙이 없어 `src/main/resources` 전체가 jar 로
패키징되고, `Dockerfile` 이 그 jar 를 이미지에 넣고, 워크플로가 이미지를 push 한다.

Docker Hub 리포지터리가 public 이었으므로 이미지를 pull 하면 누구나 `unzip app.jar` 로
DB 자격증명, JWT 서명키, Gemini API 키, Google/Apple OAuth secret, SMTP 앱 비밀번호,
VAPID private key, Sentry DSN, admin HMAC secret 을 읽을 수 있었다.
dev/prod 가 같은 시크릿을 공유했으므로 `:dev` 태그에도 prod 값이 들어있었다.

GitHub Secrets 자체는 올바른 저장소다. 문제는 워크플로가 러너에서 그 값을 꺼내
빌드 산출물에 넣은 것이며, GitHub Secrets 의 보호는 러너 주입 시점까지만 유효하다.

### git 히스토리 (종결)

`.gitignore:6` 이 `src/main/resources/application-secret.yml*` 를 막고 있으나 그 전에
커밋된 이력이 있다(`4e85106`, `a98bf71`, `888ca8c`, `0338602`, `d1052a4` 에서 추적 제거).
GitHub 리포지터리가 public 이므로 값이 노출됐다.

값이 실제로 있던 것은 `0338602`(2026-03-02)의 JWT `secret-key` 와 DB 자격증명 3세트,
`888ca8c`(2026-02-28)의 DB 자격증명 3세트다. Gemini·OAuth·SMTP·VAPID·Sentry·HMAC 은
해당 시점에 존재하지 않던 설정이라 히스토리에 없다.

**둘 다 이후 교체됐음을 사용자가 확인했으므로 이 경로는 종결됐다.** 히스토리 리라이트는
하지 않는다. 5개월간 public 이었던 값을 되돌릴 수 없어 실익이 없고, force-push 로
커밋 그래프가 재작성된다.

### 대응

| 단계 | 상태 |
| --- | --- |
| GitHub 리포지터리는 public 유지 (포트폴리오 용도), Docker Hub 는 private 전환 | 사용자 작업 |
| dev DB 보안그룹 확인 — `0.0.0.0/0` 이면 EC2 보안그룹만 허용하도록 좁힌다 | 사용자 작업 |
| 워크플로에서 시크릿을 빌드에서 분리하고 호스트 마운트로 전환 | 완료 |
| 이미지에 노출된 현재 값 전부 회전 | 사용자 작업 |

회전은 워크플로 수정 **후에** 한다. 먼저 하면 새 키가 다시 이미지에 구워진다.
4단계에서 시크릿 파일을 환경별로 새로 만들 예정이므로, 그 시점에 전 항목을 한 번에
재발급하는 것이 추가 비용이 가장 적다.

## 변경 범위

- `src/main/resources/application-prod.yml` — `app.frontend-url` 값 지정
- `src/test/java/attune/common/config/FrontendUrlProfileConfigTest.java` — 신규
- `scripts/dump-prod-seed.sh` — 신규
- `.github/workflows/deploy-prod.yml`, `deploy-dev.yml` — 시크릿을 빌드에서 분리, `environment` 키 추가, prod 로그 볼륨
- `src/main/resources/application.yml` — 마운트된 시크릿 파일 import 경로 추가
- prod 인프라(MySQL·Redis 컨테이너, 백업 cron) 생성 — 코드 외 작업
- `main` ← `develop` 머지

## 제외 범위

- Flyway/Liquibase 도입. 이번 배포 이후 별도 작업으로 검토한다.
- `LoadtestDataRunner` 및 부하테스트 기능 제거. dev 에서 계속 쓰며, prod 에는 프로파일로 차단돼 있다.
- `docs/db_schema.md` 의 `Hospital` 항목 정리. 이번 배포와 독립적이다.
- 워크플로에서 `APP_MIGRATION_DEFAULTTAGS_ENABLED` 제거. 무해하므로 별도 정리 대상.
- `2026-07-29-release-readiness-validation.md` 의 외부 연동 검증·100 VU 4시간 장기 시험. 해당 계획에서 다룬다.

## 관련 문서

- [배포 규칙](../../engineering/deployment-rules.md)
- [실사용 환경 검증 계획](2026-07-29-release-readiness-validation.md)
- [DB 스키마](../../db_schema.md)
- [데이터 규칙](../../architecture/data-rules.md)

## 관련 코드

- `.github/workflows/deploy-prod.yml`
- `src/main/resources/application-prod.yml:62` — frontend-url
- `src/main/java/attune/user/application/AccountService.java:76,142` — 메일 링크 생성
- `src/main/java/attune/common/loadtest/LoadtestDataRunner.java:33-34` — 프로파일 격리
- `docs/sql/` — 수동 적용 대상 마이그레이션 21개

## 작업 단계

### 완료

1. **`app.frontend-url` 수정** (`39ba72e`)
   - `application-prod.yml` 에 `https://attune-me.com` 지정. 같은 파일의 CORS 허용 도메인과 일치시켰다.
   - 회귀 테스트 `FrontendUrlProfileConfigTest` 추가. 기존 `ManagementServerProfileConfigTest` 의 `YamlPropertiesFactoryBean` 패턴을 그대로 따랐다. dev/prod 양쪽 값을 검사한다.

2. **덤프 스크립트 작성** (`9bfea19`, `e6ee317`)
   - `scripts/dump-prod-seed.sh` — dev 에서 `schema.sql`(전체 스키마, 데이터 없음)과 `seed.sql`(마스터 데이터)을 생성한다.
   - 마이그레이션 도구가 없으므로 `docs/sql` 21개를 순서대로 재생하는 대신 dev 의 현재 상태를 뜬다.

3. **마스터 테이블 목록 검증**
   - `docs/db_schema.md` 의 37개 테이블과 `docs/sql/*.sql` 전체를 대조했다.

   | 테이블 | 덤프 필터 | 근거 |
   | --- | --- | --- |
   | `medications` | `name <> '__loadtest_medication__'` | 약물 마스터. 부하테스트 더미 제외 |
   | `medication_dosages` | 위 약의 `medication_id` 제외 | `MedicationStrength`("약물 용량 마스터") |
   | `terms` | 없음 | 약관 마스터. `20260614_create_medication_analysis_reports.sql` 이 행을 **추가로** 넣으므로 시드 파일만으로는 불완전하다 |
   | `journal_tags` | `scope = 'SYSTEM'` | 태그 마스터. USER 행은 `owner_user_id` FK 때문에 제외 필수 |

   검토 후 제외한 후보: `ConsultationQuestion`(consultation FK), `ScheduleCategory`(user FK), `OnboardingSymptom`(user FK), `Notice`(어드민 콘텐츠, 신규 prod 는 빈 상태가 정상), `Hospital`(코드에 없음).

4. **시크릿을 이미지에서 분리** (`457c07e`, `36f834a`)
   - 빌드 전 `src/main/resources/` 에 쓰던 단계를 삭제하고, SSH 단계에서 배포 대상 호스트의 `$HOME/attune/application-secret.yml` 에 `umask 077` 로 내린다. `/opt` 대신 `$HOME` 을 쓴 이유는 sudo 가 필요 없기 때문이다.
   - 컨테이너에 `:ro` 로 마운트하고, `application.yml` 의 import 목록에 `optional:file:/app/config/application-secret.yml` 을 추가한다. `classpath:` 항목은 로컬 개발용으로 유지한다.
   - job 에 `environment: production` / `development` 를 추가한다. 환경 시크릿 미등록 상태에서는 리포 레벨 값으로 동작하므로 지금 머지해도 배포가 깨지지 않는다.
   - `deploy-dev.yml` 의 `debug: true` 를 제거한다. 시크릿 쓰기가 SSH 스크립트로 들어왔다.
   - prod 로그 디렉터리를 볼륨으로 마운트한다.

### 남은 작업

5. **시크릿 노출 대응** — [시크릿 노출](#시크릿-노출)의 대응 표 참고. Docker Hub private 전환, dev DB 보안그룹 확인, 현재 값 전부 회전.

6. **prod 인프라 생성** — 둘 다 prod EC2 에 Docker 컨테이너로 올린다. `deploy-prod.yml` 이 `--network host` 라 앱이 loopback 으로 접근한다.
   - MySQL 8.4 (`application-prod.yml` 주석 기준 버전)

     ```bash
     docker run -d --name attune_prod_db --network host --restart unless-stopped \
       -v attune_prod_db_data:/var/lib/mysql \
       -e MYSQL_ROOT_PASSWORD='...' -e MYSQL_DATABASE=attune \
       -e MYSQL_USER='...' -e MYSQL_PASSWORD='...' \
       mysql:8.4 \
       --bind-address=127.0.0.1 \
       --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci \
       --default-time-zone='+09:00'
     ```

     `--bind-address=127.0.0.1` 이 없으면 `--network host` 라 3306 이 인터넷에 열린다.
     `--default-time-zone` 을 빼면 UTC 로 뜨면서 시각이 9시간 밀린다.
     볼륨은 **이름을 붙인다**. dev(`attune_dev_db`)는 익명 볼륨이라 컨테이너를 새로 만들면
     새 빈 볼륨이 생기고 이전 데이터는 해시 디렉터리에 고아로 남는다. 에러가 없어 발견이 늦다.
   - Redis — `redis-server --bind 127.0.0.1 --protected-mode yes --requirepass '...'`
     시크릿의 prod 문서에는 `spring.data.redis.password` 항목이 **없다**(dev 에만 있음).
     `requirepass` 를 걸면 이 키를 추가해야 하며, 빠뜨리면 인증 실패로 기동하지 않는다.
   - 생성한 접속 정보를 `APPLICATION_SECRET_YML` 의 prod 문서와 일치시킨다. DB URL 은 dev 와 같이 `localhost` 를 쓴다.

7. **DB 백업 구성** — RDS 자동 백업을 대신하는 항목이다. prod EC2 crontab:

   ```bash
   0 4 * * * docker exec attune_prod_db mysqldump -uroot -p'...' \
     --single-transaction --no-tablespaces attune | gzip \
     > /backup/attune-$(date +\%F).sql.gz && \
     find /backup -name 'attune-*.sql.gz' -mtime +14 -delete
   30 4 * * * aws s3 sync /backup s3://<버킷>/db-backup/
   ```

   두 번째 줄이 없으면 EC2 가 죽을 때 백업도 같이 죽는다. 덤프는 gzip 후 수 MB 라 S3 비용은 무시할 수준이다.

8. **DB 구축**
   - 덤프 전 dev 실물 확인. 스크립트 헤더의 information_schema 쿼리로 `user_id`/`owner_user_id` 컬럼이 없는 테이블 중 데이터가 있는 것이 위 4개 외에 있는지 본다.
   - `./scripts/dump-prod-seed.sh` 실행 → `schema.sql`, `seed.sql`
   - prod EC2 로 파일을 옮긴 뒤 컨테이너 안에서 순서대로 주입한다.

9. **PR 및 머지**
   - `chore/prod-release-prep` → `develop`
   - `develop` → `main` (606 파일)

10. **배포**
   - `deploy-prod` 를 `workflow_dispatch` 로 수동 실행한다.

11. **배포 후 검증**
   - readiness 200
   - 회원가입 인증 메일과 비밀번호 재설정 메일의 링크가 `https://attune-me.com/...` 절대경로인지 실제 수신으로 확인 (1번 수정의 실효 확인)
   - 백업 cron 이 실제로 파일을 만드는지 하루 뒤 `/backup` 과 S3 양쪽 확인

## 검증 방법

- `./gradlew test --tests '*FrontendUrlProfileConfigTest*'` — 프로파일 yml 의 frontend-url 누락 검사
- `bash -n scripts/dump-prod-seed.sh` — 문법 검사
- 덤프 스크립트 자체 검사 — `seed.sql` 에 `__loadtest_medication__` 이 남으면 exit 1. 테이블별 행 수를 출력하므로 `medications 0 rows` 같은 상황을 즉시 알 수 있다(비면 앱은 뜨지만 복약 등록이 불가능하다).
- `ddl-auto: validate` — 덤프 스키마가 엔티티와 어긋나면 기동이 실패한다. 배포 성공 자체가 스키마 일치의 증거다.
- readiness 폴링 — `deploy-prod.yml` 이 최대 600초 대기하며 실패 시 컨테이너 로그를 남긴다.

## 위험 요소

| 위험 | 영향 | 완화 |
| --- | --- | --- |
| 롤백 대상 이미지 없음 | 배포 실패 시 되돌릴 곳이 없다 | 인프라·DB(4~5단계)를 완전히 끝낸 뒤 배포한다. 실패해도 실사용자가 없어 서비스 영향은 없다 |
| 시크릿의 prod 문서가 dev DB 를 가리킬 경우 | prod 컨테이너가 dev DB 에 쓴다. 기동·헬스체크가 모두 정상이라 발견이 늦는다 | 사용자가 `on-profile` 분리를 확인함. 배포 후 dev DB 에 예상치 못한 쓰기가 없는지 확인 |
| 덤프에 유저 종속 데이터 혼입 | `owner_user_id` 등 FK 제약으로 import 실패 | `journal_tags` 를 `scope='SYSTEM'` 으로 제한. 실패 시 import 단계에서 즉시 드러난다 |
| 마스터 테이블 누락 | 앱은 정상 기동하나 기능이 죽는다(예: `medications` 가 비면 복약 등록 불가) | 스크립트가 테이블별 행 수를 출력. 5단계의 information_schema 쿼리로 사전 확인 |
| 600초 내 readiness 실패 | 배포 실패 처리 | 워크플로가 `docker inspect` 와 `docker logs --tail 300` 을 남긴다 |

`cutover_jt_staging`, `legacy_journal_tag_mapping` 은 마이그레이션용 임시 테이블이며 스키마 덤프에 딸려간다. `ddl-auto: validate` 는 엔티티에 매핑되지 않은 여분 테이블을 문제 삼지 않으므로 기동에 지장이 없다.

## 롤백 방법

- **배포 전 단계** — 브랜치를 머지하지 않으면 운영에 영향이 없다.
- **배포 실패** — 실사용자가 없으므로 앞으로 고치는 것이 유일한 경로다. 컨테이너 로그로 원인을 찾아 수정 후 재배포한다.
- **2회차 이후** — 직전 이미지 태그로 `docker run` 하거나 문제 커밋을 revert 후 재배포한다([배포 규칙](../../engineering/deployment-rules.md) 5항).

## 의사결정 로그

- 2026-07-30 **DB 스키마를 dev 덤프로 구축**한다. `docs/sql` 21개를 순서대로 재생하는 방식은 중간 실패 지점을 찾기 어렵고, `20260614` 가 `terms` 에 행을 추가하는 등 시드 파일만으로는 불완전하다. 실사용자 데이터가 없어 통째 복제가 가능하다.
- 2026-07-30 **Redis 는 EC2 에 Docker 로 기동**한다. `--network host` 배포라 컨테이너 하나면 되고 추가 비용이 없다. 트래픽이 늘면 ElastiCache 로 옮긴다.
- 2026-07-31 **DB 도 RDS 대신 EC2 에 Docker 로 기동**한다. 비용 부담이 이유다. dev 가 이미 같은 구성(`attune_dev_db`)으로 돌고 있어 리소스와 구성은 검증됐다.
  RDS 를 포기하면서 잃는 것은 **자동 백업 하나**이고, 이것만 cron 두 줄로 대신한다(7단계). 실사용자의 복약 기록은 재생성이 불가능하므로 백업은 생략하지 않는다.
  트래픽이나 데이터가 커지면 RDS 로 옮긴다.
- 2026-07-30 **`LoadtestDataRunner` 는 코드에 유지**하고 덤프에서 데이터만 제외한다. 프로파일로 이미 prod 차단이 돼 있고, 삭제하면 직전에 만든 soak 부하테스트 기능(#117, #118)이 함께 죽는다.
- 2026-07-30 **`frontend-url` 은 시크릿 대신 `application-prod.yml` 에 명시**한다. 비밀이 아니고 같은 파일의 CORS 목록에 이미 같은 도메인이 있다. 프로파일 yml 이 import 된 시크릿보다 우선순위가 높아, 빈 값을 두면 시크릿에 값이 있어도 덮어쓴다.
- 2026-07-30 **GitHub 리포지터리는 public 유지, Docker Hub 는 private 전환**한다. 공개가 필요한 것은 포트폴리오로 보여지는 소스 리포지터리이고, 이미지를 pull 해서 보는 사람은 없어 Docker Hub 공개는 이득이 없다. 시크릿을 이미지에서 뺐으므로 원한다면 이미지도 공개할 수 있으나 굳이 하지 않는다.
- 2026-07-30 **시크릿 저장은 GitHub Secrets 를 유지**하고 배치 위치만 바꾼다. 호스트에 수동 배치하는 방식은 운영 부담이 늘고, AWS Secrets Manager 는 IAM 과 기동 의존성이 추가된다. 환경별 분리는 GitHub Environments 라는 기본 기능으로 해결된다.
- 2026-07-30 **git 히스토리는 리라이트하지 않는다.** 노출된 JWT 서명키와 DB 비밀번호가 이후 교체돼 값이 이미 죽었고, 5개월간 public 이었던 것을 되돌릴 수 없어 실익이 없다. force-push 로 커밋 그래프가 재작성되는 손실이 더 크다.
- 2026-07-30 메일 링크 도메인은 **non-www(`https://attune-me.com`)** 로 한다. CORS 는 www 도 허용하지만 링크는 하나만 고를 수 있고 dev 가 non-www 규칙을 쓴다. 프론트가 www 로 정규화하면 변경한다.

## 완료 조건

- [x] `app.frontend-url` prod 값 지정 및 회귀 테스트 추가
- [x] 마스터 테이블 목록을 `db_schema.md` + `docs/sql` 전체와 대조해 확정
- [x] dev→prod 덤프 스크립트 작성
- [x] 시크릿을 빌드 산출물에서 분리하고 호스트 마운트로 전환
- [x] prod 로그 볼륨 마운트
- [ ] Docker Hub 리포지터리 private 전환
- [ ] dev DB 보안그룹 확인 및 필요 시 축소
- [ ] dev 배포로 시크릿 마운트 동작 검증 (readiness 200) — prod 보다 **먼저**
- [ ] 노출된 자격증명 전부 회전 후 GitHub Environments 에 등록
- [ ] prod MySQL 8.4 컨테이너 기동 (이름 붙인 볼륨, `--bind-address=127.0.0.1`, KST) 및 시크릿 prod 문서와 접속 정보 일치
- [ ] prod Redis 컨테이너 기동 (`--bind 127.0.0.1`, `requirepass`) 및 시크릿 prod 문서에 `spring.data.redis.password` 추가
- [ ] DB 백업 cron 등록 및 S3 동기화 확인
- [ ] `schema.sql` / `seed.sql` 생성 및 prod DB 주입, 마스터 4개 테이블 행 수 확인
- [ ] `chore/prod-release-prep` → `develop` 머지
- [ ] `develop` → `main` 머지
- [ ] `deploy-prod` 실행 후 readiness 200
- [ ] 회원가입·비밀번호 재설정 메일 링크가 절대경로로 수신됨

## 작업 후 문서 업데이트 목록

- [ ] [배포 규칙](../../engineering/deployment-rules.md) — prod 최초 배포 절차와 DB 구축 방법 반영
- [ ] [DB 스키마](../../db_schema.md) — 코드에 없는 `Hospital` 항목 정리
- [ ] 이 문서를 `docs/exec-plans/completed/` 로 이동

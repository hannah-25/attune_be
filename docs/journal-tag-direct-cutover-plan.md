# Journal Tag Direct Cutover Plan

## 1. 문서 목적

일지 태그를 사용자별 복제 구조에서 공용 태그 카탈로그 구조로 완전히 전환한다.

현재 서비스에는 실제 이용자가 없으므로 다음 요구사항은 적용하지 않는다.

- 기존 태그 ID 호환
- 기존 체크 기록 보존
- 무중단 dual-write
- 단계적인 feature flag 전환
- 레거시 API 유지

기존 태그 및 체크 데이터는 초기화하고, 백엔드와 프론트엔드를 최종 구조로 한 번에 전환한다.

이 문서는 기존 `journal-tag-catalog-migration.md`의 단계적 마이그레이션 계획을 대체한다.

### 1.1 확정 전제

- 실제 이용자와 보존해야 할 일지 태그 데이터가 없다는 사실은 제품 책임자가 보장한다.
- 사용자 수와 레거시 데이터 건수 확인은 cutover 승인 조건으로 두지 않는다.
- DB 스냅샷은 데이터 보존 목적이 아니라 cutover 실패 시 기존 애플리케이션으로 복구하기 위한 기술적 안전장치다.

### 1.2 현재 저장소 기준 상태

2026-06-23 기준 저장소는 직접 전환 완료 상태가 아니라 **catalog 기반 레거시 호환 상태**다.

이미 구현된 기반:

- `JournalTag`, `UserJournalTagPreference`
- `JournalTagCatalogService`, `JournalTagCatalogCheckService`
- `/v1/journals/catalog-tags`
- additive catalog SQL과 legacy mapping/backfill SQL
- 레거시 로그의 nullable `user_id`, `journal_tag_id`

아직 직접 전환에서 완료해야 하는 핵심:

- `journal_tag_logs`와 `JournalTagLogRepository`
- `/v1/journals/tags` 최종 계약
- category별 태그·로그·API 제거
- `LegacyJournalTagMapping`과 호환 태그 생성 제거
- 일지 조회, 약물 분석, 온보딩, 회원 삭제의 통합 저장소 전환
- 일별 체크 idempotency와 preference 정규화

기존 catalog 구현을 새로 만드는 대신, 이미 구현된 catalog 모델을 최종 모델로 정리하면서 레거시 의존성을 제거한다.

---

## 2. 핵심 결정

### 2.1 단일 태그 모델 사용

시스템 기본 태그와 사용자가 생성한 태그를 모두 `journal_tags`에서 관리한다.

| 구분 | `scope` | `owner_user_id` | 설명 |
|---|---|---|---|
| 시스템 기본 태그 | `SYSTEM` | `NULL` | 모든 사용자가 사용할 수 있는 공용 태그 |
| 사용자 추가 태그 | `USER` | 생성한 사용자 ID | 생성한 사용자에게만 노출되는 개인 태그 |

### 2.2 단일 체크 로그 사용

기존의 카테고리별 로그 테이블을 하나의 `journal_tag_logs` 테이블로 통합한다.

- `condition_logs`
- `side_effect_logs`
- `trouble_logs`

카테고리는 로그가 아니라 연결된 `journal_tags.category`로 판단한다.

### 2.3 사용자 설정은 preference로 관리

`user_journal_tag_preferences`는 시스템 태그와 사용자 태그에 동일하게 적용한다.

- `enabled`: 사용자의 태그 목록에 포함할지 여부
- `visible`: 일지 빠른 입력 영역에 노출할지 여부

사용자 preference가 없으면 다음 기본값을 사용한다.

- 시스템 태그: `enabled=true`, `visible=journal_tags.default_visible`
- 사용자 태그: 생성 시 preference를 함께 만들고 `enabled=true`

### 2.4 외부 ID 통일

API에서는 `legacyTagId`, `catalogTagId`를 모두 제거한다.

태그를 나타내는 외부 필드명은 모든 API에서 `tagId`로 통일하며, 값은 `journal_tags.id`를 의미한다.

---

## 3. 목표 데이터 모델

### 3.1 `journal_tags`

```sql
CREATE TABLE journal_tags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category VARCHAR(30) NOT NULL,
    name VARCHAR(255)
        CHARACTER SET utf8mb4
        COLLATE utf8mb4_0900_as_ci
        NOT NULL,
    tag_type VARCHAR(50) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    owner_user_id BINARY(16) NULL,
    owner_key BINARY(16) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    default_visible BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_journal_tags_identity (
        scope,
        owner_key,
        category,
        name,
        tag_type
    ),
    KEY idx_journal_tags_catalog_lookup (
        scope,
        category,
        is_active
    ),
    KEY idx_journal_tags_owner_lookup (
        owner_user_id,
        category,
        is_active
    ),
    CONSTRAINT fk_journal_tags_owner
        FOREIGN KEY (owner_user_id) REFERENCES users(id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_journal_tags_category
        CHECK (category IN ('CONDITION', 'SIDE_EFFECT', 'TROUBLE')),
    CONSTRAINT ck_journal_tags_scope
        CHECK (scope IN ('SYSTEM', 'USER')),
    CONSTRAINT ck_journal_tags_scope_owner
        CHECK (
            (
                scope = 'SYSTEM'
                AND owner_user_id IS NULL
                AND owner_key = 0x00000000000000000000000000000000
            )
            OR
            (
                scope = 'USER'
                AND owner_user_id IS NOT NULL
                AND owner_key = owner_user_id
            )
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;
```

`owner_key` 규칙:

- 시스템 태그: UUID `00000000-0000-0000-0000-000000000000`
- 사용자 태그: `owner_user_id`와 같은 값

MySQL의 nullable unique index 동작에 의존하지 않고 시스템 태그와 사용자 태그의 중복을 안정적으로 방지하기 위한 컬럼이다.

### 3.2 `user_journal_tag_preferences`

```sql
CREATE TABLE user_journal_tag_preferences (
    user_id BINARY(16) NOT NULL,
    journal_tag_id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL,
    visible BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id, journal_tag_id),
    KEY idx_user_journal_tag_preferences_tag (journal_tag_id),
    CONSTRAINT fk_user_journal_tag_preferences_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_journal_tag_preferences_tag
        FOREIGN KEY (journal_tag_id) REFERENCES journal_tags(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_user_journal_tag_preferences_visible
        CHECK (enabled = TRUE OR visible = FALSE)
);
```

### 3.3 `journal_tag_logs`

```sql
CREATE TABLE journal_tag_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BINARY(16) NOT NULL,
    journal_tag_id BIGINT NOT NULL,
    journal_date DATE NOT NULL,
    checked_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_journal_tag_logs_daily_check (
        user_id,
        journal_tag_id,
        journal_date
    ),
    KEY idx_journal_tag_logs_user_date (
        user_id,
        journal_date
    ),
    KEY idx_journal_tag_logs_tag_checked_at (
        journal_tag_id,
        checked_at
    ),
    CONSTRAINT fk_journal_tag_logs_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_journal_tag_logs_tag
        FOREIGN KEY (journal_tag_id) REFERENCES journal_tags(id)
        ON DELETE RESTRICT
);
```

`journal_date`를 별도로 저장하는 이유:

- 같은 날짜에 동일 태그가 중복 체크되는 것을 DB에서 차단한다.
- 날짜별 조회 및 삭제 쿼리를 단순화한다.
- 애플리케이션 timezone 변화가 일별 유일성에 영향을 주지 않게 한다.

날짜 기준:

- 일지 날짜 경계는 사용자 최신 timezone(`user_settings.timezone`) 기준으로 판정한다. timezone이 없으면 기본값 `Asia/Seoul`을 사용한다.
- `journal_date`는 사용자가 현재 열어 둔 일지 날짜를 클라이언트가 명시적으로 전달한다.
- `checked_at`은 실제 체크 요청이 처리된 시각이다. 과거 일지를 체크해도 과거 시각으로 위조하지 않는다.
- 서버는 `journal_date`가 사용자 timezone 기준 미래가 아닌지 검증한다.
- `checked_at` 생성은 운영 JVM timezone에 의존하지 않고 주입된 `Clock`(UTC)을 사용해 실제 발생 시각을 UTC로 기록한다.

삭제 및 이력 정책:

- 사용자 삭제 시 사용자 소유 태그, preference, 로그를 제거한다.
- `journal_tags.owner_user_id`는 `ON DELETE RESTRICT`로 두고 회원 삭제 executor가 로그 → preference → 사용자 태그 → 사용자 순서를 명시적으로 지킨다.
- 태그 삭제 API는 물리 삭제가 아니라 비활성화이므로 과거 로그를 유지한다.
- 로그가 연결된 태그의 물리 삭제는 FK `RESTRICT`로 차단한다.
- `name`, `category`, `tag_type`, `scope`, `owner_user_id`는 생성 후 변경하지 않는다. 과거 로그의 의미를 바꾸는 태그 수정 API는 제공하지 않는다.

---

## 4. 시스템 태그와 사용자 태그 관리 규칙

### 4.1 시스템 기본 태그

- 애플리케이션이 제공하는 공용 태그다.
- `scope=SYSTEM`, `owner_user_id=NULL`로 저장한다.
- 일반 사용자는 태그 자체를 수정하거나 삭제할 수 없다.
- 사용자가 삭제 동작을 수행하면 전역 삭제하지 않고 자신의 preference를 `enabled=false`로 변경한다.
- 기본 노출 여부는 `default_visible`로 정의한다.
- 이번 범위에서는 시스템 태그 추가 및 이름 변경을 seed SQL로만 처리한다. 관리자 기능은 별도 요구사항이 생길 때 추가한다.

### 4.2 사용자 추가 태그

생성 시:

1. 인증 사용자 ID를 조회한다.
2. `scope=USER`로 생성한다.
3. `owner_user_id`와 `owner_key`에 인증 사용자 ID를 저장한다.
4. 같은 트랜잭션에서 preference를 생성한다.
5. 요청의 `visible` 값을 preference에 저장한다.

조회 시:

- 활성 시스템 태그 전체
- 현재 사용자가 소유한 활성 사용자 태그

두 집합만 반환한다. 다른 사용자의 개인 태그는 절대 조회하지 않는다.

수정 및 삭제 시:

- 시스템 태그는 preference만 변경할 수 있다.
- 사용자 태그는 소유자만 변경 또는 삭제할 수 있다.
- 사용자 태그 삭제는 `is_active=false`로 처리한다.
- 비활성화 이후에도 과거 체크 로그는 조회할 수 있어야 한다.
- 비활성화된 사용자 태그는 일반 목록에는 표시하지 않는다.

### 4.3 중복 규칙

시스템 태그 중복 기준:

```text
category + name + tagType
```

사용자 태그 중복 기준:

```text
ownerUserId + category + name + tagType
```

추가 규칙:

- 서로 다른 사용자는 같은 이름의 개인 태그를 만들 수 있다.
- 동일 사용자는 같은 카테고리, 이름, 타입의 활성 태그를 중복 생성할 수 없다.
- 시스템 태그와 동일한 identity의 개인 태그 생성은 거부한다.
- 중복 생성 요청에는 `409 Conflict`를 반환한다.
- 비활성화된 본인 태그와 동일한 identity를 다시 생성하면 기존 태그를 재활성화하는 방식으로 처리한다.
- 이름은 앞뒤 공백을 제거한 뒤 저장하고 빈 문자열은 거부한다.
- 이름은 Unicode NFC로 정규화한다.
- 영문 대소문자는 중복 판정에서 구분하지 않는다.
- 최초 저장 시에는 trim/NFC 적용 후 사용자가 입력한 대소문자 표기를 유지한다.
- DB identity 비교는 `utf8mb4_0900_as_ci` collation을 사용한다. 영문 대소문자는 무시하고 악센트 차이는 구분한다.

---

## 5. 최종 API 계약

기본 경로:

```text
/v1/journals/tags
```

### 5.1 태그 목록

```http
GET /v1/journals/tags?category=CONDITION
GET /v1/journals/tags?category=CONDITION&manage=true
```

`manage=true`를 포함하면 태그 관리 화면용, 생략하면 일지 입력용으로 동작한다.

응답:

```json
[
  {
    "tagId": 10,
    "category": "CONDITION",
    "name": "집중이 잘 됨",
    "tagType": "CALM",
    "scope": "SYSTEM",
    "enabled": true,
    "visible": true
  },
  {
    "tagId": 51,
    "category": "CONDITION",
    "name": "회의 중 집중 저하",
    "tagType": "USER_INPUT",
    "scope": "USER",
    "enabled": true,
    "visible": true
  }
]
```

규칙:

- `manage=true`: `enabled=false`인 시스템 태그도 반환한다. 현재 사용자의 `enabled=false` 사용자 태그(`is_active=true`)도 반환한다 (태그 관리 화면에서 재활성화할 수 있도록).
- `manage` 생략 또는 `false`: `enabled=true && visible=true`인 태그만 반환한다 (일지 입력용).
- 사용자 태그는 현재 사용자 소유 태그만 반환한다.
- 두 모드 모두 `is_active=false`인 사용자 태그는 반환하지 않는다. 같은 identity로 다시 생성하면 기존 태그를 재활성화한다.
- 정렬은 `scope(SYSTEM → USER)`, `tagId` 오름차순으로 고정한다.
- `manage`에 `true`/`false` 이외의 값이 들어오면 `400 Bad Request`를 반환한다.

### 5.2 사용자 태그 생성

```http
POST /v1/journals/tags
```

요청:

```json
{
  "category": "TROUBLE",
  "name": "회의 중 산만함",
  "tagType": "USER_INPUT",
  "visible": true
}
```

응답:

| 상황 | 상태 코드 | 응답 body |
|---|---|---|
| 신규 생성 | `201 Created` | `JournalTagResponse` |
| 비활성 태그 재활성화 | `200 OK` | `JournalTagResponse` |
| 활성 태그 중복 | `409 Conflict` | - |
| 잘못된 category/tagType 조합 | `400 Bad Request` | - |

재활성화는 기존 `journal_tags.id`를 그대로 반환한다. 클라이언트는 201/200을 구분하지 않아도 되며, 항상 응답의 `tagId`를 기준으로 동작한다.

타입 규칙:

- `SIDE_EFFECT`: `tagType=NONE`
- 사용자가 생성하는 `CONDITION`: `tagType=USER_INPUT`
- 사용자가 생성하는 `TROUBLE`: `tagType=USER_INPUT`

### 5.3 preference 변경

```http
PATCH /v1/journals/tags/{tagId}/preference
```

요청:

```json
{
  "enabled": true,
  "visible": false
}
```

검증:

- 현재 사용자가 접근할 수 없는 태그면 `404 Not Found`
- `enabled=false`이면 `visible` 요청값과 무관하게 `false`로 정규화하고 저장한다. 클라이언트 오류로 취급하지 않는다.
- `enabled`, `visible`은 둘 다 필수다. 하나라도 생략하면 `400 Bad Request`.

### 5.4 태그 삭제

```http
DELETE /v1/journals/tags/{tagId}
```

요청 파라미터는 없다. 기존 category별 삭제 API의 `journalDate`와 “해당 날짜 이후 로그 삭제” 동작은 제거하는 의도적인 breaking change다.

시스템 태그:

- 현재 사용자의 preference를 `enabled=false`, `visible=false`로 변경한다.
- 시스템 태그와 과거 체크 로그는 유지한다.

사용자 태그:

- 현재 사용자가 소유자인지 검증한다.
- `journal_tags.is_active=false`로 변경한다.
- preference를 `enabled=false`, `visible=false`로 변경한다.
- 과거 체크 로그는 유지한다.

### 5.5 태그 체크

```http
POST /v1/journals/tags/{tagId}/checks
```

요청:

```json
{
  "journalDate": "2026-06-20"
}
```

`journalDate`는 사용자가 열어 둔 일지 날짜이며 필수다. 사용자 timezone 기준 미래 날짜는 거부하고 과거 날짜와 오늘은 허용한다. 생략하거나 미래 날짜면 `400 Bad Request`를 반환한다.

응답:

```json
{
  "tagId": 10,
  "category": "CONDITION",
  "name": "집중이 잘 됨",
  "tagType": "CALM",
  "journalDate": "2026-06-20",
  "checkedAt": "2026-06-23T21:15:00"
}
```

규칙:

- 활성화되고 접근 가능한 태그만 체크할 수 있다.
- 전달받은 `journalDate`에 이미 같은 태그가 체크된 경우 새 행을 만들지 않는다.
- 중복 요청은 기존 결과를 반환하는 idempotent 동작으로 구현한다.
- 최초 생성과 중복 재요청 모두 `200 OK`를 반환한다.
- unique violation은 일반 `500`으로 전달하지 않고 동일 `(userId, tagId, journalDate)` 로그를 다시 조회해 반환한다.

### 5.6 태그 체크 해제

```http
DELETE /v1/journals/tags/{tagId}/checks?date=2026-06-20
```

`date` 파라미터는 **필수**이며 `yyyy-MM-dd` 형식이다. 사용자 timezone 기준 미래 날짜는 거부한다. 생략하거나 미래 날짜면 `400 Bad Request`를 반환한다.

응답:

- 삭제 성공 또는 이미 없음: `204 No Content`

---

## 6. 응답 DTO 설계

### 6.1 통합 태그 DTO

```java
public record JournalTagResponse(
        Long tagId,
        JournalTagCategory category,
        String name,
        String tagType,
        JournalTagScope scope,
        boolean enabled,
        boolean visible
) {}
```

### 6.2 통합 체크 DTO

```java
public record JournalTagCheckResponse(
        Long tagId,
        JournalTagCategory category,
        String name,
        String tagType,
        LocalDate journalDate,
        LocalDateTime checkedAt
) {}
```

체크 요청 DTO:

```java
public record CheckJournalTagRequest(
        @NotNull LocalDate journalDate
) {}
```

### 6.3 일지 상세 응답

프론트 변경량을 줄이기 위해 일지 응답의 카테고리별 배열 구조는 유지할 수 있다.

```json
{
  "activeTags": {
    "conditions": [],
    "sideEffects": [],
    "troubles": [],
    "goals": []
  },
  "checked": {
    "conditions": [],
    "sideEffects": [],
    "troubles": [],
    "sleep": null,
    "meal": null,
    "goals": [],
    "memo": null
  }
}
```

단, 각 태그 항목의 ID는 모두 `journal_tags.id`다.

백엔드는 통합 로그를 조회한 뒤 `JournalTagCategory`를 기준으로 세 배열에 분류한다.

---

## 7. 백엔드 구현 계획

### Phase 1. DB 전환 SQL 작성

신규 파일:

```text
docs/sql/20260623_replace_legacy_journal_tags.sql
```

지원 baseline:

- 운영/개발 DB의 정확한 적용 이력을 사전 지식으로 가정하지 않는다.
- 스크립트 시작 시 `information_schema`로 다음 두 상태 중 하나인지 판별한다.
  - `LEGACY_ONLY`: category별 태그·로그 테이블은 있고 additive catalog 테이블/컬럼은 없음
  - `ADDITIVE_CATALOG`: `20260615_create_journal_tag_catalog.sql`에 해당하는 catalog 테이블과 legacy log의 nullable catalog 컬럼이 있음
- 두 상태 모두 category별 기본 태그 행(`user_id IS NULL`)이 존재해야 한다.
- 두 상태가 아니거나 일부 객체만 존재하는 불완전 상태면 파괴적 DDL을 시작하지 않고 중단한다.
- 신규 빈 DB용 bootstrap과 기존 additive catalog DB용 cutover를 하나의 암묵적 경로로 처리하지 않는다.

스크립트 구간:

1. preflight: baseline 스키마와 현재 DB/schema 이름 확인
2. seed staging: 기존 category별 기본 태그를 persistent staging 테이블에 category/name/tagType/defaultVisible 형태로 추출
3. destructive DDL: baseline별 FK 제거 후 레거시 로그·태그·mapping/catalog 테이블 제거
4. target DDL: 최종 `journal_tags`, preference, `journal_tag_logs` 생성
5. seed: staging 테이블에서 시스템 기본 태그 삽입
6. postflight: 테이블, 컬럼, 인덱스, FK, CHECK, seed identity 검증 후 staging 테이블 제거

주의:

- 실제 운영 데이터 보존이 필요 없다는 전제에서만 사용한다.
- FK 때문에 로그 → preference/mapping → 태그 순서로 삭제한다.
- MySQL DDL은 implicit commit이 발생하므로 전체 스크립트를 하나의 트랜잭션으로 간주하지 않는다.
- staging 테이블은 부분 실패 후에도 남아 있어야 하므로 `TEMPORARY TABLE`을 사용하지 않는다. postflight 성공 후에만 제거한다.
- 각 구간의 성공 조건과 부분 실패 후 재실행 시작점을 주석으로 명시한다.
- 재실행을 지원하지 않는 구간은 명시적인 중단 조건을 둔다.
- 운영 실행 전 DB 스냅샷은 최소 1회 생성한다.

seed 데이터 결정 사항:

- 기존 DB의 category별 기본 태그(`user_id IS NULL`)를 최종 시스템 seed의 원본으로 사용한다.
- `CONDITION`은 기존 이름과 type, `SIDE_EFFECT`는 이름과 `tagType=NONE`, `TROUBLE`은 기존 이름과 type을 사용한다.
- 기존 `visible` 값은 `default_visible`로 승계한다.
- staging 과정에서 동일 identity가 중복되면 임의 병합하지 않고 preflight를 실패시킨다.
- 추출된 seed 내용은 실행 결과로 저장하고 SQL 실행 기록에 첨부한다.
- 개발 rehearsal에서 추출한 category별 seed 개수와 전체 identity checksum을 승인값으로 확정한다.
- 운영 preflight의 seed 개수/checksum이 개발 승인값과 다르면 destructive DDL 전에 중단한다.
- postflight에서 staging 승인값과 최종 `journal_tags`의 개수/checksum이 일치하는지 검증한다.

### Phase 2. 통합 도메인과 repository 구현

추가:

- `JournalTagLog`
- `JournalTagLogRepository`

`JournalTagLogRepository`가 제공할 기능:

- 사용자와 날짜 범위로 태그와 로그 조회
- 사용자와 날짜 범위의 distinct 날짜 조회
- 사용자와 날짜 범위 로그 삭제
- 사용자, 태그, 날짜로 조회
- 사용자, 태그, 날짜로 삭제
- 약물 분석용 기간 조회

"특정 날짜 이후 로그 일괄 삭제" 기능은 제공하지 않는다. 태그 삭제 API가 과거 로그를 건드리지 않는 정책으로 변경되었으므로 해당 기능이 불필요하다. 일지 기간 삭제는 "사용자와 날짜 범위 로그 삭제"로 처리한다.

권장 반환 방식:

- JPA `Tuple` 대신 명시적인 projection record 사용
- 예: `JournalTagLogView(JournalTagLog log, JournalTag tag)`

### Phase 3. 태그 서비스 통합

`JournalTagCatalogService`를 `JournalTagService`로 이름 변경하고 레거시 의존성을 제거한다.

제거할 의존성:

- `ConditionTagRepository`
- `SideEffectTagRepository`
- `TroubleTagRepository`
- `LegacyJournalTagMappingRepository`
- 카테고리별 로그 repository

서비스 책임:

- 접근 가능한 태그 목록 구성
- preference 기본값 계산
- 사용자 태그 생성 및 재활성화
- preference 변경
- 시스템 태그 비활성화
- 사용자 태그 비활성화
- 온보딩 태그 일괄 노출 설정

`JournalTagCatalogCheckService`는 `JournalTagCheckService`로 변경한다.

서비스 책임:

- 태그 접근 권한 검증
- preference 활성 상태 검증
- 통합 로그 생성
- 중복 체크 idempotency 처리
- 체크 해제

구현 규칙:

- `journalDate`는 요청값을 사용하고 `checkedAt`은 주입된 `Clock`으로 계산한다.
- `journalDate`가 사용자 timezone 기준 미래인지 검증한다.
- unique constraint 경쟁 조건은 `saveAndFlush` 실패 후 별도 조회 트랜잭션 또는 동등하게 안전한 방식으로 기존 로그를 반환한다.
- `enabled=false`인 preference는 체크를 거부한다.

### Phase 4. 레거시 서비스와 이벤트 제거

삭제 대상:

- `ConditionTagService`
- `SideEffectTagService`
- `TroubleTagService`
- `DefaultTagService`
- `DefaultTagCopyListener`
- `DefaultTagMigrationRunner`

삭제 이유:

- 시스템 태그를 사용자별로 복사하지 않는다.
- preference가 없으면 시스템 기본값을 계산할 수 있다.
- 사용자 체크 시 레거시 호환 태그를 만들 필요가 없다.

제거할 설정:

- `app.migration.default-tags.enabled`
- `app.journal-tag-catalog.dual-write-enabled`
- `app.journal-tag-catalog.read-enabled`
- `app.journal-tag-catalog.copy-defaults-enabled`
- 대응하는 환경 변수와 배포 워크플로 설정

### Phase 5. 컨트롤러와 DTO 통합

추가 또는 변경:

- `JournalTagController`
- `CreateJournalTagRequest`
- `CheckJournalTagRequest`
- `UpdateJournalTagPreferenceRequest`
- `JournalTagResponse`
- `JournalTagCheckResponse`

삭제:

- `JournalConditionController`
- `JournalSideEffectController`
- `JournalTroubleController`
- `JournalTagCatalogController`
- 카테고리별 생성 요청 DTO
- 카테고리별 태그 응답 DTO
- 카테고리별 체크 요청 및 응답 DTO
- `CatalogJournalTagResponse`
- `CatalogTagCheckResponse`

외부 계약에서 제거:

- `legacyTagId`
- `catalogTagId`

### Phase 6. 일지 조회 전환

`JournalService` 변경:

- 카테고리별 로그 repository 세 개를 `JournalTagLogRepository` 하나로 교체
- 날짜별 태그 로그를 한 번 조회
- category 기준으로 condition, sideEffect, trouble 배열 생성
- active tag 목록도 통합 태그 조회 결과에서 분류
- `legacyTagId != null` 필터 제거
- 삭제 API는 통합 로그를 사용자 ID와 날짜 범위로 삭제

단일 조회와 벌크 조회가 같은 매핑 함수를 공유하도록 구성한다.

### Phase 7. 약물 분석 전환

수정 대상:

- `AnalysisEngine`
- `MedicationChangeDetector`
- 태그 로그를 직접 조회하는 약물 분석 서비스와 테스트

변경 내용:

- `ConditionTag`, `SideEffectTag`, `TroubleTag` 타입 캐스팅 제거
- `JournalTag.name`, `JournalTag.tagType`, `JournalTag.category` 사용
- 부작용 요약 ID는 `journal_tags.id` 사용
- 카테고리별 집계는 category 필터로 처리
- 메모 키워드는 통합 태그 이름에서 생성

### Phase 8. 온보딩 전환

변경:

- `visibleCatalogTagIds` → `visibleTagIds`
- 추천 응답의 필드명을 `tagId`로 변경하고 값은 `journal_tags.id` 사용
- 추천 태그 목록은 활성 시스템 `TROUBLE` 태그에서 생성
- 저장 시 선택된 태그들의 preference를 일괄 upsert

온보딩 완료 시 기본 태그 복사 작업은 수행하지 않는다.

### Phase 9. 회원 데이터 삭제 전환

`UserDataDeletionExecutor`에서 제거:

- 카테고리별 로그 삭제
- 카테고리별 사용자 태그 삭제
- legacy mapping 삭제

새 삭제 순서:

1. `journal_tag_logs WHERE user_id = ?`
2. `user_journal_tag_preferences WHERE user_id = ?`
3. `journal_tags WHERE owner_user_id = ?`

시스템 태그는 삭제하지 않는다.

---

## 8. 프론트엔드 구현 계획

### 8.1 모델 통합

```ts
type JournalTagCategory = "CONDITION" | "SIDE_EFFECT" | "TROUBLE";
type JournalTagScope = "SYSTEM" | "USER";

interface JournalTag {
  tagId: number;
  category: JournalTagCategory;
  name: string;
  tagType: string;
  scope: JournalTagScope;
  enabled: boolean;
  visible: boolean;
}
```

`tagId`는 현재 프론트 계약과의 일관성을 위해 JSON/TypeScript `number`를 유지한다. 신규 초기화 DB의 자동 증가 ID가 `Number.MAX_SAFE_INTEGER`를 넘지 않는다는 운영 보장을 둔다. 이 범위를 넘길 가능성이 생기면 API 전체 ID를 문자열로 전환한다.

삭제:

- `legacyTagId`
- `catalogTagId`
- 카테고리별로 중복 정의된 태그 모델

### 8.2 API client 교체

기존 API 호출을 모두 `/v1/journals/tags` 기반으로 변경한다.

- 태그 관리 목록
- 사용자 태그 생성
- 표시 여부 변경
- 태그 비활성화 또는 삭제
- 체크
- 체크 해제

### 8.3 UI 동작

시스템 태그:

- 삭제 버튼 문구는 `내 목록에서 숨기기`
- 다시 활성화할 수 있도록 태그 관리 화면에는 disabled 태그도 노출

사용자 태그:

- 삭제 버튼 문구는 `태그 삭제`
- 삭제 확인 후 비활성화
- 다른 사용자에게 노출되지 않음

일지 입력 화면:

- `enabled && visible` 태그만 노출
- `tagId`를 React key와 API 요청 ID로 사용

온보딩:

- `visibleTagIds`로 전송
- 추천된 시스템 trouble 태그를 기본 선택

---

## 9. 테스트 계획

### 9.1 도메인 및 서비스 테스트

태그 목록:

- 시스템 태그와 본인 사용자 태그만 반환
- 다른 사용자 태그 미노출
- preference가 없으면 시스템 기본값 적용
- preference override 적용
- 비활성 사용자 태그 미노출

태그 생성:

- 사용자 태그와 preference가 같은 트랜잭션에서 생성
- 동일 사용자 중복 태그 `409`
- 다른 사용자의 동일 이름 태그 생성 허용
- 시스템 태그와 동일한 identity 생성 거부
- 비활성 개인 태그 재생성 시 재활성화
- 잘못된 category와 tagType 조합 거부

태그 변경 및 삭제:

- 시스템 태그는 preference만 변경
- 사용자 태그는 소유자만 비활성화 가능
- 사용자 태그 삭제 후 과거 로그 유지
- `enabled=false`일 때 `visible=false`

체크:

- 시스템 태그 체크
- 사용자 태그 체크
- disabled 태그 체크 거부
- 다른 사용자 태그 체크 거부
- 같은 날짜 중복 체크 시 행 하나만 유지
- 체크 해제 idempotency
- 과거 날짜 일지 체크 시 요청한 `journalDate`로 저장
- 사용자 timezone 기준 미래 날짜 체크 거부
- `enabled=false, visible=true` 요청이 `visible=false`로 정규화
- Asia/Seoul 자정 경계의 미래 날짜 판정
- 동시 체크 요청이 로그 한 건만 생성
- 동시 사용자 태그 재활성화

### 9.2 일지 API 테스트

- 단일 날짜 상세 조회
- 31일 이내 벌크 조회
- 날짜별 category 분류
- active tag category 분류
- 일지 단일 삭제
- 일지 기간 삭제
- 태그가 비활성화되어도 과거 체크 이름 조회

### 9.3 분석 테스트

- condition 이름별 distinct day 집계
- side effect 이름별 distinct day 집계
- trouble type별 distinct day 집계
- 시간대별 집계
- 부작용 요약의 태그 ID 및 이름
- 메모 키워드 추출

### 9.4 회원 삭제 테스트

- 사용자의 통합 로그 삭제
- preference 삭제
- 로그 선행 삭제 후 사용자 소유 태그 삭제 가능 (`journal_tag_logs → journal_tags` FK RESTRICT 검증)
- 사용자 소유 태그 삭제
- 시스템 태그 유지
- FK 위반 없이 사용자 삭제 완료

### 9.5 구조 검증

다음 문자열이 운영 코드에서 0건이어야 한다.

```text
LegacyJournalTagMapping
legacyTagId
catalogTagId
ConditionTag
SideEffectTag
TroubleTag
condition_tag_id
side_effect_tag_id
trouble_tag_id
```

예외:

- 과거 마이그레이션 문서
- 폐기된 SQL을 보관하기로 결정한 경우의 문서 기록

### 9.6 DB와 계약 검증

- MySQL 8.4에서 `LEGACY_ONLY`, `ADDITIVE_CATALOG` baseline 각각으로 cutover SQL 실행
- cutover SQL 부분 실패 후 문서화된 재실행 절차 검증
- `information_schema` postflight 결과와 Hibernate `ddl-auto=validate` 일치
- OpenAPI 또는 controller contract test로 `tagId`와 `/v1/journals/tags` 고정
- H2 자동 생성 스키마만으로 DB 제약을 검증하지 않고 MySQL 통합 테스트를 별도로 실행
- 배포 workflow에서 `-x test`를 제거하거나 테스트가 선행된 동일 commit SHA만 배포하도록 gate 추가

---

## 10. 배포 계획

이용자가 없으므로 개발과 운영 모두 짧은 점검 시간 동안 직접 전환한다.

현재 `develop`과 `main` push는 자동 배포를 실행하므로 schema-dependent 중간 PR을 해당 브랜치에 그대로 merge하면 안 된다. cutover 완료 전까지 비배포 release branch를 사용하거나 push 배포 trigger를 임시 비활성화한다.

배포 아티팩트:

- 백엔드와 프론트는 cutover 전에 각 저장소의 release commit SHA를 확정하고 조합 검증한다.
- Docker image는 `dev`/`prod` 같은 mutable tag만 사용하지 않고 commit SHA tag를 함께 발행한다.
- DB 변경 후에는 미리 검증된 SHA image만 배포한다.
- 점검 종료 전까지 구 프론트와 새 백엔드가 섞여 호출되지 않도록 UI와 API 트래픽을 모두 maintenance 상태로 유지한다.

### 10.1 개발 환경

1. release commit과 backend/frontend SHA 아티팩트 확정
2. 전체 테스트와 MySQL cutover rehearsal 통과
3. 개발 UI/API maintenance 활성화 및 애플리케이션 중지
4. 개발 DB 스냅샷 생성과 복구 가능 여부 확인
5. direct cutover SQL preflight 실행
6. destructive/target DDL과 seed 실행
7. postflight와 seed checksum 확인
8. 새 백엔드 SHA image 배포
9. 개발 profile도 일시적으로 `ddl-auto=validate`로 기동 확인
10. 새 프론트 배포
11. 수동 smoke test 후 maintenance 해제

### 10.2 운영 환경

1. 개발 환경 rehearsal과 복구 rehearsal 완료
2. 운영 release commit과 backend/frontend SHA 아티팩트 확정
3. 운영 UI/API maintenance 활성화 및 애플리케이션 중지
4. 운영 DB 스냅샷 생성
5. direct cutover SQL preflight 실행
6. destructive/target DDL과 seed 실행
7. postflight와 seed checksum 확인
8. 새 백엔드 SHA image 배포
9. DB 연결을 포함한 readiness와 `ddl-auto=validate` 성공 확인
10. 새 프론트 배포
11. 핵심 API smoke test
12. 오류율과 로그를 확인한 뒤 maintenance 해제

### 10.3 복구 절차

DDL 시작 이후에는 구버전 백엔드만 재배포해서 복구할 수 없다.

복구 조건:

- cutover SQL postflight 실패
- 새 백엔드 기동 또는 `ddl-auto=validate` 실패
- 핵심 smoke test 실패
- 권한 오류, 5xx, unique violation이 합의한 허용치 이상 발생

복구 순서:

1. maintenance 유지
2. 새 애플리케이션 중지
3. cutover 직전 DB 스냅샷 복원
4. 이전 backend/frontend commit SHA 아티팩트 배포
5. 레거시 API와 DB 연결 smoke test
6. 정상 확인 후 maintenance 해제

스냅샷 이후 생성된 데이터는 복구 시 유실된다. 실제 이용자가 없다는 확정 전제 때문에 이 손실을 허용한다.

### 10.4 필수 smoke test

1. 신규 사용자 가입 또는 테스트 사용자 활성화
2. 기본 태그 목록 조회
3. 사용자 태그 생성
4. 시스템 태그 및 사용자 태그 체크
5. 과거 날짜 일지를 열고 해당 `journalDate`로 체크
6. 일지 상세 조회
7. 체크 해제
8. preference 변경
9. 사용자 태그 삭제
10. 온보딩 추천 저장
11. 약물 분석 입력 데이터 조회

---

## 11. 작업 단위와 권장 PR 순서

### PR 1. 통합 DB 및 도메인

- direct cutover SQL
- `JournalTagLog`
- `JournalTagLogRepository`
- repository 테스트

완료 조건:

- cutover SQL이 `LEGACY_ONLY`, `ADDITIVE_CATALOG` baseline 각각에서 성공
- 불완전한 schema baseline에서 destructive DDL 전에 중단
- 기존 기본 태그의 identity와 `default_visible`이 staging을 거쳐 동일하게 승계
- `ddl-auto=validate` 성공

### PR 2. 통합 태그 API

- `JournalTagService`
- `JournalTagCheckService`
- `JournalTagController`
- 통합 DTO
- API 테스트

완료 조건:

- 시스템 태그와 사용자 태그의 전체 CRUD 및 체크 동작

### PR 3. 일지·분석·온보딩 전환

- `JournalService`
- `AnalysisEngine`
- `MedicationChangeDetector`
- 온보딩 요청 및 응답
- 회원 데이터 삭제

완료 조건:

- 카테고리별 기존 기능이 통합 저장소로 동작

주의: PR 3 merge 시점까지 `ConditionTagService`, `SideEffectTagService`, `TroubleTagService`와 카테고리별 컨트롤러는 코드에 남아 있다. release branch에서만 통합하므로 이 상태가 자동 배포되지 않는다. PR 4에서 최종 제거한다.

### PR 4. 레거시 제거

- 카테고리별 엔티티, repository, service, controller 삭제
- mapping 및 default copy 코드 삭제
- feature flag 삭제
- 테스트 정리
- 기존 마이그레이션 문서 폐기 표시

완료 조건:

- 레거시 참조 검색 결과 0건
- 전체 테스트 통과

PR 4는 PR 3이 완료된 뒤에만 시작한다. PR 3에서 레거시 서비스 의존성을 먼저 제거해야 PR 4의 삭제가 컴파일 오류 없이 가능하다.

### PR 5. 프론트엔드 전환

- 통합 모델
- API client 교체
- 일지 및 태그 관리 UI
- 온보딩 요청 변경

완료 조건:

- 프론트에서 레거시 태그 API 및 ID를 사용하지 않음

**배포 전제:**

PR 1~5는 자동 배포되지 않는 release branch에서 통합하고 검증한다. 새 코드가 `journal_tag_logs`만 참조하는 시점부터는 구버전 DB에서 기동할 수 없으므로 `develop`/`main` 자동 배포 브랜치에 중간 상태를 merge하지 않는다.

cutover SQL과 새 백엔드·프론트 배포는 단일 점검 시간 안에 진행한다.

```
[release branch 통합 및 SHA 아티팩트 검증 완료]
  → 점검 시작
  → UI/API maintenance
  → DB snapshot
  → cutover SQL 실행
  → 새 backend SHA 배포
  → 새 frontend SHA 배포
  → smoke test
  → monitoring
  → 점검 종료
```

---

## 12. 위험 요소와 대응

### DB와 애플리케이션 배포 순서 불일치

새 백엔드는 새 테이블만 사용하므로 구버전 DB에서 기동할 수 없다.

대응:

- 자동 배포 브랜치에는 완성 전 schema-dependent 코드를 merge하지 않는다.
- maintenance → 애플리케이션 중지 → SQL → 새 백엔드 SHA 배포 순서를 고정한다.
- generic health check 외에 DB 연결과 실제 `journal_tags`, `journal_tag_logs` 조회를 포함한 readiness를 사용한다.

### MySQL DDL 부분 실패

MySQL DDL은 implicit commit 때문에 중간 실패 시 자동 rollback되지 않는다.

대응:

- cutover SQL을 preflight, destructive DDL, target DDL/seed, postflight로 분리한다.
- 각 구간의 완료 marker와 재실행 조건을 문서화한다.
- postflight 이전에는 maintenance를 해제하지 않는다.
- 문서화되지 않은 중간 상태에서는 임의 수정을 계속하지 않고 스냅샷 복구를 선택한다.

### 비활성 태그의 과거 로그 조회 실패

태그 목록 조회와 로그 조회를 같은 활성 조건으로 구현하면 과거 기록의 이름이 사라질 수 있다.

대응:

- 일반 태그 목록만 `is_active=true`로 제한한다.
- 과거 로그 조인에서는 비활성 태그도 조회한다.

### 중복 체크 경쟁 조건

애플리케이션의 사전 조회만으로는 동시 요청을 완전히 방지할 수 없다.

대응:

- `(user_id, journal_tag_id, journal_date)` unique constraint를 최종 방어선으로 사용한다.
- unique violation 발생 시 기존 로그를 조회하여 반환한다.
- transaction rollback-only 상태에서 같은 트랜잭션으로 재조회하지 않도록 구현하고 MySQL 동시성 테스트로 검증한다.

### 사용자 태그 권한 누락

ID만으로 태그를 조회하면 다른 사용자의 개인 태그를 수정하거나 체크할 수 있다.

대응:

모든 태그 접근은 다음 조건을 만족해야 한다.

```text
scope = SYSTEM
OR
(scope = USER AND owner_user_id = currentUserId)
```

### 시스템 태그와 사용자 태그 이름 중복

같은 의미의 데이터가 서로 다른 ID로 집계될 수 있다.

대응:

- 사용자 태그 생성 전에 동일 identity의 활성 시스템 태그를 조회한다.
- 존재하면 새 태그 생성을 거부하고 시스템 태그 활성화를 안내한다.

### 비활성 태그 재활성화 경쟁 조건

두 요청이 동시에 같은 비활성 태그를 재활성화하려 하면 둘 다 UPDATE를 실행하게 된다. unique constraint는 INSERT에만 작동하므로 UPDATE 중복을 막지 못한다. 결과는 동일(재활성화)이지만 여러 번 DB를 수정할 수 있다.

대응:

- `is_active=false` 태그를 reactivate할 때 `SELECT FOR UPDATE`로 먼저 잠금을 획득하거나, 낙관적 잠금(`@Version`)을 사용한다.
- 낙관적 잠금을 선택하면 충돌 시 한 번 재조회/재시도하고, 계속 충돌하면 `409 Conflict`를 반환한다.
- 비활성 행이 없는 상태에서 동시 생성되는 경우 unique violation을 `409 Conflict`로 변환한다.

### preference와 개인 태그 소유권 무결성

DB FK만으로는 사용자가 다른 사용자의 `scope=USER` 태그를 preference 또는 로그에서 참조하는 것을 막지 못한다.

대응:

- 모든 생성/변경/체크 경로에서 시스템 태그 또는 본인 소유 태그 조건을 강제한다.
- postflight와 smoke test에 타 사용자 개인 태그 참조 탐지 쿼리를 포함한다.
- `enabled=false`이면 `visible=false`가 되도록 서비스 정규화와 DB CHECK를 함께 적용한다.

---

## 13. 완료 조건

### 데이터베이스

- [ ] `journal_tags`가 시스템 태그와 사용자 태그를 모두 관리한다.
- [ ] `user_journal_tag_preferences`가 사용자별 enabled/visible을 관리한다.
- [ ] `journal_tag_logs`가 모든 태그 체크를 관리한다.
- [ ] 카테고리별 레거시 태그·로그 테이블이 제거되었다.
- [ ] legacy mapping 테이블이 제거되었다.
- [ ] 시스템 기본 태그 seed가 적용되었다.
- [ ] `owner_key`와 scope/owner 관계가 CHECK constraint로 강제된다.
- [ ] seed category별 개수와 checksum이 확정값과 일치한다.
- [ ] MySQL 8.4 cutover SQL preflight/postflight가 통과한다.

### 백엔드

- [ ] 외부 API가 `tagId=journal_tags.id`만 사용한다.
- [ ] 시스템 태그와 사용자 태그 권한이 구분된다.
- [ ] 사용자 태그가 생성자에게만 노출된다.
- [ ] 일지 조회와 삭제가 통합 로그를 사용한다.
- [ ] 약물 분석이 통합 로그를 사용한다.
- [ ] 온보딩이 `visibleTagIds`를 사용한다.
- [ ] 회원 삭제가 통합 테이블을 정리한다.
- [ ] dual-write와 feature flag가 제거되었다.
- [ ] 레거시 코드 참조가 없다.
- [ ] 체크 API가 일별 idempotency를 보장한다.
- [ ] 과거 날짜 체크가 요청한 `journalDate`에 저장되고 `checkedAt`은 실제 처리 시각으로 남는다.
- [ ] 태그 삭제가 과거 로그를 삭제하지 않는다.
- [ ] 사용자 timezone 기준 날짜 경계 테스트가 통과한다.

### 프론트엔드

- [ ] 통합 태그 모델을 사용한다.
- [ ] `legacyTagId`, `catalogTagId`를 사용하지 않는다.
- [ ] 모든 태그 API가 `/v1/journals/tags`를 사용한다.
- [ ] 시스템 태그와 사용자 태그의 삭제 UX가 구분된다.
- [ ] 온보딩이 `visibleTagIds`를 전송한다.

### 검증

- [ ] 전체 백엔드 테스트가 통과한다.
- [ ] 배포 workflow가 테스트를 생략하지 않거나 동일 SHA의 선행 테스트 통과를 강제한다.
- [ ] 프론트 lint, typecheck, build가 통과한다.
- [ ] 개발 DB에서 `ddl-auto=validate` 기동에 성공한다.
- [ ] DB 연결과 통합 태그 테이블 조회를 포함한 readiness가 성공한다.
- [ ] 개발 환경 smoke test를 통과한다.
- [ ] 운영 DB 스냅샷을 생성했다.
- [ ] 스냅샷 복구 rehearsal을 완료했다.
- [ ] backend/frontend commit SHA 아티팩트를 보관했다.
- [ ] 운영 배포와 smoke test를 완료했다.

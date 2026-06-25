# 데이터 규칙

## 엔티티 / 영속성

1. **새 엔티티/테이블/컬럼 추가 전 [`../db_schema.md`](../db_schema.md) 를 먼저 확인한다.**
   - 이미 정의되어 있으면 그 스키마를 그대로 따른다.
   - 정의와 다르거나 문서에 없으면 → **작업 전 사람에게 확인**.
2. 엔티티는 `domain/model` 에만 둔다. 비즈니스 식별/불변식은 엔티티 또는 도메인 서비스에서.
3. Repository(`domain/repository`)는 데이터 접근만. 비즈니스 정책을 Repository에 넣지 않는다.
4. 커스텀 조회는 `findAllInRangeWithTag` 처럼 의도가 드러나는 이름을 쓴다.

## 트랜잭션 / OSIV

- **OSIV 비활성화**(`open-in-view: false`). 지연 로딩은 **서비스 메서드(트랜잭션) 안에서만** 가능하다.
- 컨트롤러/DTO 매핑 시점에 지연 로딩하면 예외가 난다. 필요한 연관은 쿼리(fetch join 등)로 미리 로딩한다.
- 쓰기 유스케이스는 `@Transactional`, 읽기는 `@Transactional(readOnly = true)` 를 기본으로 한다.
- 배치 insert/update 설정(`batch_size: 50`, `order_inserts/updates`)이 켜져 있으니 대량 저장은 saveAll을 활용한다.

## 내부 모델 ↔ 외부 응답 분리

- 엔티티를 컨트롤러 응답으로 직접 노출하지 않는다. `application/dto` 의 응답 DTO로 변환한다.
- 요청 DTO에서 엔티티로 매핑하는 책임은 Service(또는 매퍼)에 둔다.

## 강제 수단

- 새 엔티티는 `docs/db_schema.md` 와 대조하는 것을 PR 체크리스트로 강제한다.
- `scripts/agent/generate-data-schema` 로 [`../generated/data-schema.md`](../generated/data-schema.md)(엔티티 인벤토리)를 갱신해 드리프트를 줄인다.
- ASSUMPTION: 현재 스키마 마이그레이션 도구(Flyway/Liquibase) 미사용 → JPA `ddl-auto` 기반. 도입 검토는 tech-debt-tracker 참고.

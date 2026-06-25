# PR 리뷰 체크리스트

사람·에이전트 공통. 머지 전 모두 만족해야 한다.

## 정확성 / 요구사항
- [ ] 요구사항을 충족하는가
- [ ] 기존 동작을 보존하는가(의도치 않은 회귀 없음)
- [ ] 엣지/실패 케이스를 다루는가

## 아키텍처 / 도메인
- [ ] 계층 규칙 준수(adapter → application → domain) — [dependency-rules](../architecture/dependency-rules.md)
- [ ] 모듈 레이아웃 준수 — [module-rules](../architecture/module-rules.md)
- [ ] 도메인 규칙 위반 없음
- [ ] 엔티티 직접 노출 없이 DTO 사용

## 데이터
- [ ] 새 엔티티/컬럼이 `db_schema.md` 와 일치(없던 변경이면 사람 확인 받음)
- [ ] 마이그레이션/데이터 영향 검토(OSIV·트랜잭션 경계 포함)

## API
- [ ] 비호환 변경 없음(있으면 사람 확인 + 문서화)
- [ ] 상태코드/오류 응답이 규칙(`ErrorResponse`)에 맞음

## 보안
- [ ] 인증/인가 적용이 올바름(어드민 분리 포함)
- [ ] secret/token/개인정보 미커밋·미로그
- [ ] 입력 검증(Bean Validation) 적용

## 예외 처리
- [ ] 커스텀 예외가 기반 예외 상속 + GlobalExceptionHandler 매핑

## 테스트
- [ ] 변경에 대응하는 테스트 추가/갱신
- [ ] `scripts/agent/verify` 통과(빌드+테스트+아키텍처)

## 문서
- [ ] [doc-gardening 매핑](./doc-gardening-workflow.md)대로 문서 갱신
- [ ] generated 문서는 스크립트로 갱신(손수정 아님)

## 운영 / 롤백
- [ ] 배포·환경변수 영향 검토
- [ ] 성능 영향 검토
- [ ] 롤백 가능성 확인

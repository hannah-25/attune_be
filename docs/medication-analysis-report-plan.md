# 약물 치료 경과 리포트 구현 계획

스펙 문서: [`medication-analysis-report-spec.md`](./medication-analysis-report-spec.md)

---

## 결정이 필요한 사항 (작업 전 확인)

| 항목 | 결정 |
| --- | --- |
| 모듈 위치 | 별도 `medicationAnalysis` 모듈 |
| AI 동의 저장 | 기존 `term` 모듈 인프라 활용 — `TermType`에 `AI_ANALYSIS_CONSENT` 추가 |
| OUTDATED 감지 시점 | 리포트 조회 시 `MAX(updated_at)` 경량 비교, DB 갱신은 재생성 요청 시 |
| Gemini 모델 | Gemini 1.5 Flash |
| 프롬프트 버전 관리 | 코드 상수 (MVP) |

---

## Phase 1. AI 동의 관리

**기존 `term` 모듈 인프라 활용. 신규 테이블 없음.**

동의 이력은 `UserTermAgreement`에 쌓이며, 현재 상태는 해당 유저의 `AI_ANALYSIS_CONSENT` 타입 최신 레코드의 `is_agreed`로 판단합니다.

### 작업 목록

- [x] `TermType`에 `AI_ANALYSIS_CONSENT` 추가
- [ ] DB 초기 데이터: `terms` 테이블에 `type = 'AI_ANALYSIS_CONSENT'` 레코드 삽입 — 어드민 API(`POST /v1/admin/terms`)로 직접 삽입 필요
- [x] `PUT /v1/ai-analysis-consent` — `UserTermAgreement` 생성 (`is_agreed = true`)
- [x] `DELETE /v1/ai-analysis-consent` — `UserTermAgreement` 생성 (`is_agreed = false`)
- [x] 현재 동의 상태 조회 내부 메서드: `AI_ANALYSIS_CONSENT` 타입 최신 레코드 `is_agreed` 반환 (리포트 생성 시 호출)

---

## Phase 2. 모듈 골격

**새 모듈 `attune/medicationAnalysis/` 생성.**

```
attune/medicationAnalysis/
  adapter/web/
    MedicationAnalysisController.java
  application/
    MedicationAnalysisService.java
    dto/request/
    dto/response/
  domain/
    model/
      MedicationAnalysisReport.java      ← 리포트 엔티티
      ReportStatus.java                  ← PENDING / COMPLETED / FAILED / OUTDATED
    repository/
      MedicationAnalysisReportRepository.java
  infrastructure/
    GeminiClient.java                    ← Gemini API 호출
    GeminiResponseValidator.java         ← 검증 파이프라인
```

### 작업 목록

- [x] `medication_analysis_reports` 테이블 생성 및 `db_schema.md` 반영 (`docs/sql/20260614_create_medication_analysis_reports.sql`)
- [x] `MedicationAnalysisReport` 엔티티
- [x] `ReportStatus` enum (PENDING / COMPLETED / FAILED / OUTDATED)
- [x] `MedicationAnalysisReportRepository`
- [x] 컨트롤러·서비스 껍데기 (API 라우팅만)

---

## Phase 3. 서버 분석 엔진

분석 계산 로직 전체. Gemini 없이 동작해야 함.

### 3-1. 예정 일정 생성 및 상태 판정

- [x] 분석 기간 내 `UserMedicationSchedule` × 날짜 수 = 전체 예정 일정 목록 생성
- [x] 각 예정 일정에 `TAKEN` / `SKIPPED` / `UNRECORDED` 판정

### 3-2. 복용 통계

- [x] 복용 기록률 계산 (`TAKEN / 전체`)
- [x] 복용 여부 기록률 계산 (`(TAKEN + SKIPPED) / 전체`)

### 3-3. 날짜 그룹 분류

- [x] 날짜별 TAKEN·SKIPPED·UNRECORDED 집계
- [x] 우선순위에 따라 날짜를 3개 그룹으로 분류 (스펙 §7)
- [x] 그룹별 최소 3일 조건 체크

### 3-4. 날짜 그룹별 일지 비교

- [x] 그룹별 `DailyGoalLog` 점수 평균
- [x] 그룹별 `ConditionLog` 태그별 기록일 수
- [x] 그룹별 `SideEffectLog` 태그별 기록일 수
- [x] 그룹별 `TroubleLog` 타입별 기록일 수
- [x] 그룹별 `DailyStatusLog` 수면 시간 평균, 수면 질 분포, 식사 여부 비율

### 3-5. 3시간 시간대 집계

- [x] `ConditionLog.checkedAt` → 시간대 버킷 배치
- [x] `SideEffectLog.checkedAt` → 시간대 버킷 배치
- [x] `TroubleLog.checkedAt` → 시간대 버킷 배치
- [x] `UserMedicationLog.takenAt` → 시간대 버킷 배치
- [x] 동일 날짜·구간·태그 중복 제거 (1일 1회로 집계)
- [x] 기록일 3일 미만 구간 제외 처리

### 3-6. 데이터 품질 검사

- [x] 전체 일지 기록일 7일 미만 → 리포트 생성 불가 판정
- [x] 각 분석 항목별 조건 미충족 시 `limitations` 목록에 이유 추가

---

## Phase 4. 변경 감지

### 4-1. 상담 연결 변경 감지

- [x] 분석 기간 내 `UserMedication` 수집 (상담 연결 여부로 confirmed/estimated 구분)
- [x] 동일 약의 직전 레코드와 비교하여 ADD / DOSE_CHANGE / SWITCH / CONTINUE 판정

### 4-2. 상담 연결 없는 변경 추정

- [x] `startedAt` 기준으로 직전 레코드 탐색
- [x] 약·용량 비교로 변경 유형 추정

### 4-3. 변경 전후 비교

- [x] 변경 기준일 결정 (우선순위 적용)
- [x] 전후 기간 길이 균등화
- [x] 전후 각각 7일 이상, 일지 기록일 3일 이상 조건 체크
- [x] 조건 충족 시 전/후 기간 일지 비교 계산

---

## Phase 5. 스냅샷 생성 및 해시

- [x] Phase 3·4 결과를 스냅샷 JSON 구조로 직렬화
- [x] 근거 ID 부여 (TIME_WINDOW_01 형식)
- [x] 메모 후보 선정: 증상·부작용 키워드 포함 메모 발췌
- [x] `source_data_hash` 계산 (SHA-256, 기간 내 모든 로그 id+timestamp 정렬 후 해시)

---

## Phase 6. Gemini 연동

### 6-1. 클라이언트 구현

- [x] Gemini API 키 기존 설정 활용 (`GeminiProperties`, `AiTextGenerator`)
- [x] `GeminiReportClient` — 스냅샷 JSON을 받아 구조화 응답 반환
- [x] 프롬프트 구성 (역할 지시 + 스냅샷 JSON + JSON 응답 스키마)

### 6-2. 검증 파이프라인

- [x] JSON 파싱 검증 (마크다운 펜스 제거 포함)
- [x] `evidenceIds`가 스냅샷에 존재하는지 검증
- [x] 금지 표현 목록 검증 (정규식)
- [x] 검증 실패 시 `ai_result_json = null`, 기본 통계 리포트만 제공

---

## Phase 7. 리포트 저장 및 재생성

- [x] `POST /v1/medication-analysis/reports` 전체 플로우 연결
  1. 기간 유효성 검사
  2. 데이터 품질 검사
  3. 해시 계산 → 기존 리포트 재사용 여부 판단
  4. 스냅샷 생성
  5. Gemini 호출 (동의한 경우)
  6. 저장
- [x] OUTDATED 처리 — 단건 조회 시 해시 재계산으로 경량 비교 (이벤트 없음)
- [x] `GET /v1/medication-analysis/reports/{reportId}` — 단건 조회
- [x] `GET /v1/medication-analysis/reports` — 목록 조회

---

## Phase 8. 조회 보조 API

- [x] `GET /v1/medication-analysis/availability`
  - 기간 내 일지 기록일 수, 리포트 생성 가능 여부, 불가 시 이유 반환
- [x] `GET /v1/medication-analysis/summary`
  - 리포트 생성 없이 복용 기록률·여부 기록률만 빠르게 반환

---

## Phase 9. 문서 갱신

- [x] `docs/db_schema.md` — `medication_analysis_reports` 테이블 추가, `terms.type` 컬럼에 `AI_ANALYSIS_CONSENT` 추가
- [x] `docs/api-guide.md` — 신규 엔드포인트 7개 추가 (AI 동의 2 + 리포트 5)
- [x] `CLAUDE.md` 도메인 모듈 표 — `medicationAnalysis` 모듈 추가

---

## 의존 관계 요약

```
Phase 1 (동의)
    ↓
Phase 2 (모듈 골격)
    ↓
Phase 3 (분석 엔진) ── Phase 4 (변경 감지)
    ↓                        ↓
Phase 5 (스냅샷·해시)
    ↓
Phase 6 (Gemini)
    ↓
Phase 7 (저장·재생성)
    ↓
Phase 8 (조회 API) ── Phase 9 (문서)
```

Phase 3와 Phase 4는 병렬 진행 가능합니다.

---

## MVP 제외 항목 (추후 검토)

- 상담 일자 기반 빠른 기간 선택 (프론트 UX, 백엔드는 동일 API 사용)
- 리포트 내 근거 날짜 클릭 → 해당 일지 이동 (딥링크)
- Gemini 프롬프트 버전 DB 관리
- 부작용 증가·감소 경향 (선형 회귀 등 추가 계산 필요)

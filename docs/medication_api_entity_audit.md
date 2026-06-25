# Medication API vs Entity 구조 검토 결과

작성일: 2026-05-30  
범위: `src/main/java/attune/medication` 전체 API/Service/DTO/Repository + consultation 연계 조회

## 1) 결론

- 엔티티 구조 위배: **없음**
- 이전 점검에서 식별한 의미 혼동 항목 2건: **수정 완료**

## 2) 구조 정합성 체크

| 체크 항목 | 결과 | 근거 |
|---|---|---|
| `UserMedication`가 `medication_id` 대신 `medication_dosage_id` 참조 | PASS | `@JoinColumn(name = "medication_dosage_id")` ([UserMedication.java](/D:/hannah-dev/attune-be/src/main/java/attune/medication/domain/model/UserMedication.java)) |
| `consultation_id` nullable 허용 | PASS | `@JoinColumn(name = "consultation_id")` + null 분기 생성 로직 ([UserMedication.java](/D:/hannah-dev/attune-be/src/main/java/attune/medication/domain/model/UserMedication.java), [MedicationService.java](/D:/hannah-dev/attune-be/src/main/java/attune/medication/application/MedicationService.java)) |
| `MedicationDosage` 유니크 `(medication_id, amount)` | PASS | 엔티티 유니크 제약 ([MedicationDosage.java](/D:/hannah-dev/attune-be/src/main/java/attune/medication/domain/model/MedicationDosage.java)) |
| `UserMedicationSchedule` 유니크 `(user_medication_id, dose_time)` | PASS | 엔티티 유니크 제약 ([UserMedicationSchedule.java](/D:/hannah-dev/attune-be/src/main/java/attune/medication/domain/model/UserMedicationSchedule.java)) |
| Create API가 `medicationDosageId` 기반으로 생성 | PASS | DTO/서비스 반영 ([CreateMedicationRequest.java](/D:/hannah-dev/attune-be/src/main/java/attune/medication/application/dto/request/CreateMedicationRequest.java), [MedicationService.java](/D:/hannah-dev/attune-be/src/main/java/attune/medication/application/MedicationService.java)) |
| 구 구조 필드(`hospital_id`, `alarm_active`, `quantity`, `medication_strength_id`) 미사용 | PASS | medication/consultation 패키지 검색 결과 사용 흔적 없음 |
| 로그 조회 조인 경로가 `user_medication -> medication_dosage -> medication` | PASS | JPQL join fetch 반영 ([UserMedicationLogRepository.java](/D:/hannah-dev/attune-be/src/main/java/attune/medication/domain/repository/UserMedicationLogRepository.java)) |
| consultation 소유권 검증(`user_id`, `is_deleted`) | PASS | `findByIdAndUser_IdAndIsDeletedFalse` 사용 ([ConsultationRepository.java](/D:/hannah-dev/attune-be/src/main/java/attune/consultation/domain/repository/ConsultationRepository.java), [MedicationService.java](/D:/hannah-dev/attune-be/src/main/java/attune/medication/application/MedicationService.java)) |

## 3) 수정 완료 항목

### A. 응답 필드명 의미 정합화

- `UpdateMedicationResponse.medicationId` -> `userMedicationId`로 변경  
  ([UpdateMedicationResponse.java](/D:/hannah-dev/attune-be/src/main/java/attune/medication/application/dto/response/UpdateMedicationResponse.java))

- `MedicationPeriodLogResponse.LogEntry.medicationId` -> `userMedicationId`로 변경  
  ([MedicationPeriodLogResponse.java](/D:/hannah-dev/attune-be/src/main/java/attune/medication/application/dto/response/MedicationPeriodLogResponse.java))

### B. `QuickLogRequest`의 `POSTPONE` + `scheduleId` 필수 정책

- `scheduleId`를 nullable로 변경하고, 서비스에서 조건부 검증 적용:
  - `action == POSTPONE`이면 `scheduleId` 없이 허용
  - 그 외 액션은 `scheduleId` 필수
- 반영 파일:
  - [QuickLogRequest.java](/D:/hannah-dev/attune-be/src/main/java/attune/medication/application/dto/request/QuickLogRequest.java)
  - [MedicationService.java](/D:/hannah-dev/attune-be/src/main/java/attune/medication/application/MedicationService.java)
  - [InvalidQuickLogRequestException.java](/D:/hannah-dev/attune-be/src/main/java/attune/common/error/badrequest/InvalidQuickLogRequestException.java)

## 4) 참고 DDL 반영 상태

- 스크립트에도 동일 설계 반영:
  - `(medication_id, amount)` 유니크
  - `consultation_id BIGINT NULL`
  - `(user_medication_id, dose_time)` 유니크
  - `hospital_id/alarm_active/quantity` 제거  
  ([20260530_medication_schema_refactor.sql](/D:/hannah-dev/attune-be/docs/sql/20260530_medication_schema_refactor.sql))

## 5) API 경로 명확화(혼동 제거)

- 사용자 복약 리소스 경로를 `medications`에서 `user-medications`로 분리.
- 기준 약(마스터) 조회만 `medications/standards` 유지.

| 기능 | 기존 | 변경 |
|---|---|---|
| 복약 등록 | `POST /v1/medications` | `POST /v1/user-medications` |
| 복약 수정 | `PATCH /v1/medications/{userMedicationId}` | `PATCH /v1/user-medications/{userMedicationId}` |
| 복약 로그 조회(단건) | `GET /v1/medications/{userMedicationId}/logs` | `GET /v1/user-medications/{userMedicationId}/logs` |
| 복약 로그 조회(기간) | `GET /v1/medications/logs` | `GET /v1/user-medications/logs` |
| 빠른 복약 기록 | `POST /v1/medications/{userMedicationId}/log/quick` | `POST /v1/user-medications/{userMedicationId}/log/quick` |
| 기준 약 상세 | `GET /v1/medications/standards/{medicationId}` | 동일 |

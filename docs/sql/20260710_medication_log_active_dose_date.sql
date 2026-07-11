-- 활성 복용 로그의 "스케줄당 하루 1건" 불변식을 DB 유니크 제약으로 승격.
--
-- 배경: 기존 제약은 (user_medication_schedule_id, taken_at)인데 taken_at이 마이크로초
-- 단위 now()라 같은 날 두 건이 들어오는 것을 막지 못했다. 불변식은 응용 코드의 범위
-- 조회로만 지켜지고 있어 동시 요청에 취약했다.
--
-- active_dose_date는 (is_active, taken_at)에서 파생된다. 활성이면 DATE(taken_at),
-- 비활성이면 NULL. MySQL은 유니크 인덱스에서 NULL을 서로 다른 값으로 취급하므로
-- 비활성(취소된) 로그는 같은 날 몇 건이 쌓여도 제약에 걸리지 않는다.
--
-- Apply manually to production before deploying with ddl-auto=validate.

-- 1) 컬럼 추가
ALTER TABLE user_medication_logs
  ADD COLUMN active_dose_date DATE NULL AFTER is_active;

-- 2) 백필: 활성 로그에만 복용일을 채운다.
UPDATE user_medication_logs
   SET active_dose_date = DATE(taken_at)
 WHERE is_active = TRUE;

-- 3) [필수 선행 점검] 이미 (스케줄, 복용일)이 중복된 활성 로그가 있는지 확인한다.
--    응용 레벨 경합으로 생겼을 수 있으며, 남아 있으면 4)의 유니크 키 추가가 실패한다.
--    결과가 0건이면 4)로 진행한다.
--
-- SELECT user_medication_schedule_id, active_dose_date, COUNT(*) AS cnt
--   FROM user_medication_logs
--  WHERE is_active = TRUE
--  GROUP BY user_medication_schedule_id, active_dose_date
-- HAVING cnt > 1;
--
--    중복이 있다면 가장 최근 1건만 남기고 나머지를 비활성화한다.
--    (복용 기록을 삭제하지 않고 비활성으로 내린다 — 리포트는 활성 로그만 집계한다.)
--
-- UPDATE user_medication_logs l
--   JOIN (
--          SELECT user_medication_schedule_id, active_dose_date, MAX(id) AS keep_id
--            FROM user_medication_logs
--           WHERE is_active = TRUE
--           GROUP BY user_medication_schedule_id, active_dose_date
--          HAVING COUNT(*) > 1
--        ) d
--     ON l.user_medication_schedule_id = d.user_medication_schedule_id
--    AND l.active_dose_date = d.active_dose_date
--    SET l.is_active = FALSE,
--        l.active_dose_date = NULL
--  WHERE l.is_active = TRUE
--    AND l.id <> d.keep_id;

-- 4) 무용한 기존 제약을 내리고 새 제약을 건다.
ALTER TABLE user_medication_logs
  DROP INDEX uk_user_medication_logs_schedule_taken_at,
  ADD UNIQUE KEY uk_user_medication_logs_active_dose (user_medication_schedule_id, active_dose_date);

-- 참고: journal_tag_logs의 uk_journal_tag_logs_daily_check는 이미 운영에 존재한다
-- (20260623_replace_legacy_journal_tags.sql). 엔티티에 누락돼 있어 테스트 DB에만
-- 없었으며, 이번 커밋에서 엔티티 선언을 추가했다. 운영 스키마 변경은 없다.

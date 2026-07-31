-- 복약 기록에 "기록 당시 timezone"을 보존한다.
--
-- 배경: taken_at은 기록 시점 사용자 timezone의 현지 벽시계다(절대 시각이 아니다).
-- 어느 timezone의 벽시계인지를 남기지 않으면 실제 복용 순간을 복원할 수 없다.
--
-- 필요한 이유(멱등성): 오프라인 큐는 at-least-once로 재전송한다. 재전송이 도착하기 전에
-- 사용자가 timezone을 바꾸면(예: 뉴욕 출장 후 귀국) 같은 절대 시각이 다른 복용일로 계산돼
-- 기존 로그를 찾지 못하고 두 번째 활성 로그가 생긴다. uk_user_medication_logs_active_dose는
-- 복용일이 다르므로 막지 못한다. 결과적으로 한 번 먹은 약이 이틀치로 기록된다.
-- dose_timezone이 있으면 taken_at과 합쳐 원래 복용 순간을 복원해 같은 복용임을 식별할 수 있다.
--
-- nullable로 두는 이유: 롤링 배포 중 구 버전 인스턴스는 이 컬럼을 쓰지 않는다. NOT NULL이면
-- 구 버전의 INSERT가 실패한다. 애플리케이션은 NULL을 'Asia/Seoul'로 해석한다.
--
-- Apply manually to production before deploying with ddl-auto=validate.

-- 1) 컬럼 추가
ALTER TABLE user_medication_logs
  ADD COLUMN dose_timezone VARCHAR(64) NULL AFTER taken_at;

-- 2) 백필: 기존 기록은 모두 서버 고정 KST 기준으로 저장됐다.
UPDATE user_medication_logs
   SET dose_timezone = 'Asia/Seoul'
 WHERE dose_timezone IS NULL;

-- 참고: NOT NULL 승격은 하지 않는다. 위의 롤링 배포 이유 때문이며,
-- 애플리케이션이 NULL을 'Asia/Seoul'로 해석하므로 의미상 공백이 없다.

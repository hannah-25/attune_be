-- Web Push 도달 상태 추적: 구독별 delivery, 시도별 attempt 테이블 추가.
-- 계획: docs/exec-plans/active/2026-07-17-web-push-delivery-tracking.md
--
-- Apply manually to production before deploying with ddl-auto=validate.

-- 1) notification_history: 알림함 이동 경로와 읽음 시각.
--    url이 NULL이면 애플리케이션이 '/home'으로 해석한다(기존 행 백필 불필요).
--    read_at은 OPENED(클릭)과 별개로, 알림함에서 실제로 읽음 처리한 최초 시각이다.
ALTER TABLE notification_history
  ADD COLUMN url VARCHAR(255) NULL AFTER body,
  ADD COLUMN read_at DATETIME(6) NULL AFTER status;

-- 알림함 상태 필터(readAt IS NULL/NOT NULL) + sentAt/id cursor pagination 지원.
CREATE INDEX idx_notification_history_user_read_sent
  ON notification_history (user_id, read_at, sent_at DESC, id DESC);

-- 2) notification_deliveries: 한 NotificationHistory를 한 활성 구독에 보내는 전체 단위.
--    provider_accepted_at 등은 하위 attempt의 최초 시각을 요약한 값이다(PR2에서 채운다).
CREATE TABLE notification_deliveries (
  id BINARY(16) NOT NULL,
  notification_history_id BIGINT NOT NULL,
  subscription_id BIGINT NOT NULL,
  provider_accepted_at DATETIME(6) NULL,
  received_at DATETIME(6) NULL,
  displayed_at DATETIME(6) NULL,
  opened_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_notification_deliveries_history_subscription (notification_history_id, subscription_id),
  KEY idx_notification_deliveries_subscription (subscription_id),
  CONSTRAINT fk_notification_deliveries_history
    FOREIGN KEY (notification_history_id) REFERENCES notification_history(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_notification_deliveries_subscription
    FOREIGN KEY (subscription_id) REFERENCES notification_subscriptions(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

-- 3) notification_delivery_attempts: 실제 외부 전송 시도마다 한 행.
--    receipt_token_hash만 저장하고 토큰 평문은 저장하지 않는다(SHA-256 hex, 64자).
--    receipt_expires_at 이전까지만 서비스 워커 영수증을 수락한다(PR2/PR3에서 사용).
CREATE TABLE notification_delivery_attempts (
  id BINARY(16) NOT NULL,
  delivery_id BINARY(16) NOT NULL,
  attempt_no INT NOT NULL,
  receipt_token_hash CHAR(64) NOT NULL,
  receipt_expires_at DATETIME(6) NOT NULL,
  provider_accepted_at DATETIME(6) NULL,
  received_at DATETIME(6) NULL,
  displayed_at DATETIME(6) NULL,
  opened_at DATETIME(6) NULL,
  failed_at DATETIME(6) NULL,
  failure_reason VARCHAR(100) NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_notification_delivery_attempts_delivery_no (delivery_id, attempt_no),
  CONSTRAINT fk_notification_delivery_attempts_delivery
    FOREIGN KEY (delivery_id) REFERENCES notification_deliveries(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

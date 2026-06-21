CREATE TABLE admin_audit_logs (
    id BINARY(16) NOT NULL,
    action VARCHAR(40) NOT NULL,
    target_reference VARCHAR(128) NOT NULL,
    target_label VARCHAR(200) NULL,
    admin_id BINARY(16) NOT NULL,
    admin_email VARCHAR(320) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_admin_audit_logs_created_at (created_at DESC, id DESC),
    INDEX idx_admin_audit_logs_target_reference (target_reference)
);

-- WITHDRAWAL_CANCELLED:
--   target_reference = "member_{UUID}"
--   target_label = the member nickname
--
-- MEMBER_DELETED:
--   target_reference = the full, unpadded Base64URL value of
--     HMAC-SHA256(admin.audit.hmac-secret, "member:{UUID}")
--   target_label = NULL
--
-- STATUS_CHANGED:
--   target_reference = "member_{UUID}"
--   target_label = the member nickname
--
-- Do not put a member's name, email, phone number, raw UUID, or other personal
-- information in MEMBER_DELETED target_label or reason.

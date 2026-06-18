-- medication_analysis_reports 테이블 생성
CREATE TABLE medication_analysis_reports
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BINARY(16)   NOT NULL,
    period_start     DATE         NOT NULL,
    period_end       DATE         NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    source_data_hash VARCHAR(64)  NOT NULL,
    snapshot_json    LONGTEXT,
    ai_result_json   LONGTEXT,
    model_name       VARCHAR(100),
    prompt_version   VARCHAR(20),
    generated_at     DATETIME     NOT NULL,
    KEY idx_mar_user_generated_at (user_id, generated_at DESC),
    UNIQUE KEY uk_mar_user_period_source (user_id, period_start, period_end, source_data_hash),
    CONSTRAINT fk_mar_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- AI_ANALYSIS_CONSENT 약관 초기 데이터 (어드민 배포 후 삽입 필요)
-- POST /v1/admin/terms 로 대체 가능
-- INSERT INTO terms (version, type, content, effective_at, created_at)
-- VALUES (1, 'AI_ANALYSIS_CONSENT', '[AI 분석 동의 약관 내용]', NOW(), NOW());

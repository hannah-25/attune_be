-- Add tables for OnboardingGoalSnapshot and ConsultationQuestion entities.
-- These were missing from previous migrations and required for ddl-auto=validate to pass.

CREATE TABLE IF NOT EXISTS onboarding_goal_snapshots (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  user_id      BINARY(16)  NOT NULL,
  daily_goal_id BIGINT     NOT NULL,
  onboarding_time DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_onboarding_goal_snapshots_user (user_id),
  KEY idx_onboarding_goal_snapshots_goal (daily_goal_id),
  CONSTRAINT fk_onboarding_goal_snapshots_goal
    FOREIGN KEY (daily_goal_id) REFERENCES daily_goals(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS consultation_questions (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  consultation_id BIGINT        NOT NULL,
  text            VARCHAR(255)  NOT NULL,
  created_at      DATETIME(6)   NULL,
  PRIMARY KEY (id),
  KEY idx_consultation_questions_consultation (consultation_id),
  CONSTRAINT fk_consultation_questions_consultation
    FOREIGN KEY (consultation_id) REFERENCES consultations(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- Medication Schema Refactor (2026-05-30)
-- Target: medication_consultation_model.md
-- Assumption: this environment has no meaningful production data.
-- =========================================================

SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------
-- 1) Drop legacy tables (destructive)
-- ---------------------------------------------------------
DROP TABLE IF EXISTS user_medication_logs;
DROP TABLE IF EXISTS user_medication_schedules;
DROP TABLE IF EXISTS user_medications;
DROP TABLE IF EXISTS medication_strengths;
DROP TABLE IF EXISTS medication_dosages;

-- ---------------------------------------------------------
-- 2) Create medication_dosages (new)
--    unit/form removed by decision.
-- ---------------------------------------------------------
CREATE TABLE medication_dosages (
  id BIGINT NOT NULL AUTO_INCREMENT,
  medication_id BIGINT NOT NULL,
  amount DECIMAL(6,2) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id),
  UNIQUE KEY uk_medication_dosages_medication_amount (medication_id, amount),
  KEY idx_medication_dosages_medication_id (medication_id),
  CONSTRAINT fk_medication_dosages_medication
    FOREIGN KEY (medication_id) REFERENCES medications(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- 3) Create user_medications
--    - medication_id removed
--    - consultation_id nullable
--    - medication_dosage_id added
--    - hospital_id, alarm_active removed
--
-- NOTE:
--   This uses BINARY(16) for user_id to match the current UUID mapping.
--   If your users.id physical type is different (e.g. CHAR(36)),
--   change user_id type below accordingly.
-- ---------------------------------------------------------
CREATE TABLE user_medications (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BINARY(16) NOT NULL,
  consultation_id BIGINT NULL,
  medication_dosage_id BIGINT NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  started_at DATE NULL,
  end_at DATE NULL,
  created_at DATETIME(6) NULL,
  updated_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  KEY idx_user_medications_user_id (user_id),
  KEY idx_user_medications_consultation_id (consultation_id),
  KEY idx_user_medications_medication_dosage_id (medication_dosage_id),
  CONSTRAINT fk_user_medications_user
    FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_user_medications_consultation
    FOREIGN KEY (consultation_id) REFERENCES consultations(id),
  CONSTRAINT fk_user_medications_medication_dosage
    FOREIGN KEY (medication_dosage_id) REFERENCES medication_dosages(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- 4) Create user_medication_schedules
--    - medication_strength_id removed
--    - quantity removed
--    - unique(user_medication_id, dose_time) added
-- ---------------------------------------------------------
CREATE TABLE user_medication_schedules (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_medication_id BIGINT NOT NULL,
  dose_time TIME NOT NULL,
  label VARCHAR(100) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_medication_schedules_user_medication_time (user_medication_id, dose_time),
  KEY idx_user_medication_schedules_user_medication_id (user_medication_id),
  CONSTRAINT fk_user_medication_schedules_user_medication
    FOREIGN KEY (user_medication_id) REFERENCES user_medications(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- 5) Recreate user_medication_logs
-- ---------------------------------------------------------
CREATE TABLE user_medication_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_medication_schedule_id BIGINT NOT NULL,
  taken_at DATETIME(6) NOT NULL,
  status VARCHAR(50) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_medication_logs_schedule_taken_at (user_medication_schedule_id, taken_at),
  KEY idx_user_medication_logs_schedule_id (user_medication_schedule_id),
  CONSTRAINT fk_user_medication_logs_schedule
    FOREIGN KEY (user_medication_schedule_id) REFERENCES user_medication_schedules(id)
) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;


<!-- 자동 생성: scripts/agent/generate-data-schema. 수동 수정 금지. -->
# 엔티티 인벤토리 (자동 생성)

생성 시각: 2026-06-25 19:57:03
권위 있는 스키마: docs/db_schema.md. 이 파일은 코드↔문서 드리프트 점검용이다.

| 도메인 | 엔티티 | @Table |
|--------|--------|--------|
| `admin` | AdminAuditLog | admin_audit_logs |
| `alarm` | NotificationHistory | (기본) |
| `alarm` | NotificationSubscription | (기본) |
| `calendar` | CalendarConnection | (기본) |
| `calendar` | ExternalCalendarEvent | (기본) |
| `communityBoard` | Comment | comments |
| `communityBoard` | CommunityBoard | community_boards |
| `consultation` | Consultation | consultations |
| `consultation` | ConsultationQuestion | consultation_questions |
| `journal` | DailyGoal | (기본) |
| `journal` | DailyGoalLog | (기본) |
| `journal` | DailyStatusLog | (기본) |
| `journal` | JournalTag | (기본) |
| `journal` | JournalTagLog | journal_tag_logs |
| `journal` | Memo | (기본) |
| `journal` | OnboardingGoalSnapshot | onboarding_goal_snapshots |
| `journal` | UserJournalTagPreference | user_journal_tag_preferences |
| `medication` | Medication | (기본) |
| `medication` | MedicationDosage | (기본) |
| `medication` | UserMedication | user_medications |
| `medication` | UserMedicationLog | (기본) |
| `medication` | UserMedicationSchedule | (기본) |
| `medication` | MedicationDosageRepository | (기본) |
| `medicationAnalysis` | MedicationAnalysisReport | medication_analysis_reports |
| `notice` | Notice | notices |
| `onboarding` | AsrsAssessment | asrs_assessments |
| `onboarding` | OnboardingSymptom | onboarding_symptoms |
| `schedule` | Schedule | schedules |
| `schedule` | ScheduleAlarm | (기본) |
| `schedule` | ScheduleCategory | schedule_categories |
| `support` | SupportInquiry | support_inquiries |
| `term` | Term | terms |
| `term` | UserTermAgreement | user_term_agreements |
| `todo` | Todo | (기본) |
| `user` | EmailVerificationToken | (기본) |
| `user` | PasswordResetToken | (기본) |
| `user` | User | users |
| `user` | UserSetting | user_settings |

총 엔티티: 38

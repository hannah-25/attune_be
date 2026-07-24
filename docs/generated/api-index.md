<!-- 자동 생성: scripts/agent/generate-api-index. 수동 수정 금지. -->
# API 인덱스 (자동 생성)

생성 시각: 2026-07-24 18:07:10
출처: 컨트롤러 @*Mapping 스캔. 권위 있는 명세는 /swagger-ui.html, /v3/api-docs.

### `admin` — AdminAuditLogController
```
@RequestMapping(ApiVersion.V1 + "/admin/audit-logs")
@GetMapping
```

### `admin` — AdminMarketingController
```
@RequestMapping(ApiVersion.V1 + "/admin/marketing")
@PostMapping("/push")
```

### `admin` — AdminMemberController
```
@RequestMapping(ApiVersion.V1 + "/admin/members")
@GetMapping
@PostMapping("/{memberId}/status")
@PostMapping("/{memberId}/withdrawal/cancel")
@PostMapping("/{memberId}/withdrawal/complete")
@PostMapping("/{memberId}/withdrawal/soft-delete")
```

### `ai` — AiController
```
@RequestMapping(ApiVersion.V1 + "/ai")
@PostMapping("/generate")
```

### `alarm` — AdminNotificationController
```
@RequestMapping(ApiVersion.V1 + "/admin/notices")
@PostMapping("/{noticeId}/push")
```

### `alarm` — NotificationInboxController
```
@RequestMapping(ApiVersion.V1 + "/notifications")
@GetMapping
@PatchMapping("/{notificationHistoryId}/read")
```

### `alarm` — NotificationSubscriptionController
```
@RequestMapping(ApiVersion.V1 + "/alarm/subscriptions")
@PostMapping
@GetMapping
@DeleteMapping
```

### `auth` — AuthController
```
@RequestMapping(ApiVersion.V1 + "/auth")
@PostMapping("/login")
@PostMapping("/reissue")
@PostMapping("/logout")
@PostMapping("/restore")
@PostMapping("/social/restore")
@PostMapping("/social/login")
```

### `calendar` — CalendarConnectionController
```
@RequestMapping(ApiVersion.V1 + "/calendar-connections")
@GetMapping
@PostMapping("/google")
@PostMapping("/{connectionId}/sync")
@DeleteMapping("/{connectionId}")
```

### `calendar` — CalendarEventController
```
@RequestMapping(ApiVersion.V1 + "/calendar/events")
@GetMapping
```

### `common` — HealthController
```
@RequestMapping(ApiVersion.V1 + "/health")
@GetMapping
@GetMapping("/db")
```

### `communityBoard` — CommentController
```
@RequestMapping(ApiVersion.V1 + "/community")
@GetMapping("/posts/{postId}/comments")
@PostMapping("/posts/{postId}/comments")
@PatchMapping("/comments/{commentId}")
@DeleteMapping("/comments/{commentId}")
```

### `communityBoard` — CommunityBoardController
```
@RequestMapping(ApiVersion.V1 + "/community")
@PostMapping("/posts")
@GetMapping("/posts")
@GetMapping("/posts/{postId}")
@PutMapping("/posts/{postId}")
@DeleteMapping("/posts/{postId}")
```

### `consultation` — ConsultationController
```
@RequestMapping(ApiVersion.V1 + "/consultations")
@PostMapping
@DeleteMapping("/{consultationId}")
@DeleteMapping("/{consultationId}/result")
@GetMapping("/{consultationId}")
@GetMapping("/{consultationId}/questions")
@PostMapping("/{consultationId}/questions")
@DeleteMapping("/{consultationId}/questions/{questionId}")
@PatchMapping("/{consultationId}/result")
@GetMapping
@PatchMapping("/{consultationId}")
```

### `journal` — JournalController
```
@RequestMapping(ApiVersion.V1 + "/journals")
@GetMapping("/{date}")
@GetMapping
@GetMapping("/dates")
@DeleteMapping("/{date}")
@DeleteMapping
```

### `journal` — JournalDailyStatusController
```
@RequestMapping(ApiVersion.V1 + "/journals/{date}/sleep-meal")
@PostMapping
```

### `journal` — JournalGoalController
```
@RequestMapping(ApiVersion.V1 + "/journals")
@PostMapping("/goals")
@DeleteMapping("/goals/{goalId}")
@PatchMapping("/goals/{goalId}")
@PostMapping("/{date}/goals")
```

### `journal` — JournalMemoController
```
@RequestMapping(ApiVersion.V1 + "/journals/{date}/memo")
@GetMapping
@PostMapping
```

### `journal` — JournalTagController
```
@RequestMapping(ApiVersion.V1 + "/journals/tags")
@GetMapping
@PostMapping
@PatchMapping("/{tagId}/preference")
@DeleteMapping("/{tagId}")
@PostMapping("/{tagId}/checks")
@DeleteMapping("/{tagId}/checks")
```

### `medication` — MedicationController
```
@RequestMapping(ApiVersion.V1)
@GetMapping("/user-medications")
@GetMapping("/medications/standards/{medicationId}")
@GetMapping("/medications")
@PostMapping("/user-medications")
@PatchMapping("/user-medications/{userMedicationId}")
@GetMapping("/user-medications/{userMedicationId}/logs")
@GetMapping("/user-medications/logs")
@PostMapping("/user-medications/{userMedicationId}/log/quick")
```

### `medicationAnalysis` — MedicationAnalysisController
```
@RequestMapping(ApiVersion.V1 + "/medication-analysis")
@GetMapping("/availability")
@GetMapping("/summary")
@PostMapping("/reports")
@GetMapping("/reports/{reportId}")
@GetMapping("/reports")
```

### `notice` — AdminNoticeController
```
@RequestMapping(ApiVersion.V1 + "/admin/notices")
@PostMapping
@PatchMapping("/{noticeId}")
@DeleteMapping("/{noticeId}")
```

### `notice` — NoticeController
```
@RequestMapping(ApiVersion.V1 + "/notices")
@GetMapping
@GetMapping("/{noticeId}")
```

### `onboarding` — OnboardingController
```
@RequestMapping(ApiVersion.V1 + "/onboarding")
@GetMapping("/history")
@GetMapping("/history/{id}")
@GetMapping("/status")
@PostMapping("/asrs")
@PostMapping("/symptoms")
@PostMapping("/ai-recommendations")
@PostMapping("/goals")
@PostMapping("/complete")
@PostMapping("/skip")
```

### `schedule` — ScheduleCategoryController
```
@RequestMapping(ApiVersion.V1 + "/schedule-categories")
@GetMapping
@PostMapping
@PatchMapping("/{categoryId}")
@DeleteMapping("/{categoryId}")
```

### `schedule` — ScheduleController
```
@RequestMapping(ApiVersion.V1 + "/schedules")
@PostMapping
@GetMapping
@GetMapping("/{scheduleId}")
@PatchMapping("/{scheduleId}")
@DeleteMapping("/{scheduleId}")
@PutMapping("/{scheduleId}/alarms")
```

### `support` — SupportInquiryController
```
@RequestMapping(ApiVersion.V1 + "/support/inquiries")
@PostMapping
```

### `term` — AdminTermController
```
@RequestMapping(ApiVersion.V1 + "/admin/terms")
@GetMapping
@PostMapping
```

### `term` — AiAnalysisConsentController
```
@RequestMapping(ApiVersion.V1 + "/ai-analysis-consent")
@PutMapping
@DeleteMapping
```

### `term` — TermController
```
@RequestMapping(ApiVersion.V1 + "/terms")
@GetMapping("/latest")
```

### `todo` — TodoController
```
@RequestMapping(ApiVersion.V1 + "/todos")
@PostMapping
@GetMapping
@GetMapping("/{todoId}")
@PatchMapping("/{todoId}")
```

### `user` — AccountController
```
@RequestMapping(ApiVersion.V1 + "/account")
@PostMapping("/signup")
@GetMapping("/verify-email")
@PatchMapping("/password")
@PostMapping("/password/reset")
@GetMapping("/password/reset/{token}")
@PostMapping("/password/reset/confirm")
@PostMapping("/withdraw")
```

### `user` — UserProfileController
```
@RequestMapping(ApiVersion.V1 + "/users/me")
@GetMapping("/profile")
@PutMapping("/nickname")
@PostMapping("/image")
```

### `user` — UserSettingController
```
@RequestMapping(ApiVersion.V1 + "/users/settings")
@GetMapping
@PatchMapping
```


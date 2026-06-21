package attune.alarm.application;

import attune.alarm.domain.model.NotificationAlarmType;
import attune.alarm.domain.model.NotificationStatus;
import attune.todo.domain.model.Todo;
import attune.todo.domain.repository.TodoRepository;
import attune.user.domain.model.UserSetting;
import attune.user.domain.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class TodoAlarmScheduler {

    private final TodoRepository todoRepository;
    private final UserSettingRepository userSettingRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void sendTodoAlarms() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        List<Todo> candidates = loadCandidates(now);
        if (candidates.isEmpty()) return;

        Map<UUID, UserSetting> settingsByUser = loadSettings(candidates);

        for (Todo todo : candidates) {
            try {
                UserSetting setting = settingsByUser.get(todo.getUserId());
                if (setting == null || !setting.isTodoNotification()) continue;

                NotificationStatus status = notificationService.sendToUser(
                        todo.getUserId(),
                        NotificationAlarmType.TODO,
                        todo.getId(),
                        todo.getDueAt(),
                        new PushMessage(todo.getText(), "할 일 마감 시간이에요.", "/calendar")
                );
                if (status == NotificationStatus.SENT || status == NotificationStatus.SKIPPED) {
                    todoRepository.markAlarmSentById(todo.getId());
                }
            } catch (Exception e) {
                log.error("Todo 알림 발송 실패 - todoId={}", todo.getId(), e);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Todo> loadCandidates(LocalDateTime now) {
        return todoRepository.findAlarmCandidates(now.minusHours(1), now.plusMinutes(1));
    }

    @Transactional(readOnly = true)
    public Map<UUID, UserSetting> loadSettings(List<Todo> candidates) {
        List<UUID> userIds = candidates.stream().map(Todo::getUserId).distinct().toList();
        return userSettingRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserSetting::getId, us -> us));
    }
}

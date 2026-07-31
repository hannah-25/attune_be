package attune.alarm.application.dto.response;

/**
 * 구독별 전송 시도 중 알림함에 표시할 가장 진전된 상태다.
 * 실제 기기 표시나 사용자의 인지를 보장하지 않는다.
 */
public enum NotificationDeliveryStatus {
    PROVIDER_ACCEPTED,
    RECEIVED,
    DISPLAYED,
    OPENED
}

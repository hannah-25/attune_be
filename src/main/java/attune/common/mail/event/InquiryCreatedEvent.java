package attune.common.mail.event;

public record InquiryCreatedEvent(String email, String type, String title, String content) {
}

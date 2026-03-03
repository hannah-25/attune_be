package attune.common.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendPasswordResetEmail(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("[Attune] 비밀번호 재설정 안내");
        message.setText(
                "안녕하세요, Attune입니다.\n\n" +
                "아래 링크를 클릭하여 비밀번호를 재설정해주세요.\n\n" +
                resetLink + "\n\n" +
                "링크는 발송 후 24시간 동안 유효합니다.\n" +
                "본인이 요청하지 않으셨다면 이 메일을 무시하세요."
        );
        mailSender.send(message);
    }
}
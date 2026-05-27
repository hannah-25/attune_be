package attune.common.mail;

import attune.common.error.internalserver.MailSendFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private void sendEmail(String to, String subject, String html, String replyTo) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            if (replyTo != null) helper.setReplyTo(replyTo);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new MailSendFailedException(e);
        }
    }



    //sendEmail 메서드들
    public void sendVerificationEmail(String to, String nickname, String verificationLink) {
        sendEmail(to, "[Attune] 이메일 인증을 완료해주세요", buildVerificationHtml(nickname, verificationLink), null);
    }

    public void sendWelcomeEmail(String to, String nickname) {
        sendEmail(to, "[Attune] 회원가입을 축하합니다!", buildWelcomeHtml(nickname), null);
    }

    public void sendPasswordResetEmail(String to, String nickname, String resetLink) {
        sendEmail(to, "[Attune] 비밀번호 재설정 안내", buildPasswordResetHtml(nickname, resetLink), null);
    }

    public void sendTermsUpdateEmail(String to, String title, String htmlContent) {
        sendEmail(to, "[Attune] " + title, wrapWithLayout(htmlContent), null);
    }

    public void sendInquiryEmail(String replyTo, String type, String title, String content) {
        try {
            sendEmail(fromEmail, "[Attune 문의] " + title, buildInquiryHtml(replyTo, type, content), replyTo);
            log.info("문의 이메일 발송 완료");
        } catch (Exception e) {
            log.error("문의 이메일 발송 실패 — error={}", e.getMessage(), e);
        }
    }


    private String wrapWithLayout(String bodyHtml) {
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background:#f5f5f5;font-family:'Apple SD Gothic Neo',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr>
                      <td align="center" style="padding:40px 0;">
                        <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:8px;overflow:hidden;">

                          <!-- 헤더 -->
                          <tr>
                            <td style="background:#1a1a2e;padding:28px 40px;">
                              <span style="color:#ffffff;font-size:22px;font-weight:700;letter-spacing:-0.5px;">Attune</span>
                            </td>
                          </tr>

                          <!-- 본문 -->
                          <tr>
                            <td style="padding:44px 40px 32px;">
                              %s
                            </td>
                          </tr>

                          <!-- 구분선 -->
                          <tr>
                            <td style="padding:0 40px;">
                              <hr style="border:none;border-top:1px solid #eeeeee;margin:0;">
                            </td>
                          </tr>

                          <!-- 푸터 -->
                          <tr>
                            <td style="padding:24px 40px;background:#fafafa;">
                              <p style="margin:0;font-size:11px;color:#aaa;line-height:1.8;">
                                이 메일은 Attune 서비스 약관 변경 안내를 위해 발송된 메일로, 수신 동의 여부와 무관하게 발송되었습니다.<br>
                                이 메일은 발신 전용으로 회신하실 수 없습니다.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(bodyHtml);
    }


    // html 생성 메서드들

    private String buildInquiryHtml(String replyTo, String type, String content) {
        return wrapWithLayout("""
                <p style="margin:0 0 8px;font-size:13px;color:#888;">문의 유형</p>
                <p style="margin:0 0 24px;font-size:15px;color:#222;">%s</p>

                <p style="margin:0 0 8px;font-size:13px;color:#888;">연락처</p>
                <p style="margin:0 0 24px;font-size:15px;color:#222;">%s</p>

                <p style="margin:0 0 8px;font-size:13px;color:#888;">문의 내용</p>
                <p style="margin:0;font-size:15px;color:#444;line-height:1.8;white-space:pre-wrap;">%s</p>
                """.formatted(type, replyTo, content));
    }

    private String buildVerificationHtml(String nickname, String verificationLink) {
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background:#f5f5f5;font-family:'Apple SD Gothic Neo',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr>
                      <td align="center" style="padding:40px 0;">
                        <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:8px;overflow:hidden;">

                          <!-- 헤더 -->
                          <tr>
                            <td style="background:#1a1a2e;padding:28px 40px;">
                              <span style="color:#ffffff;font-size:22px;font-weight:700;letter-spacing:-0.5px;">Attune</span>
                            </td>
                          </tr>

                          <!-- 본문 -->
                          <tr>
                            <td style="padding:44px 40px 32px;">
                              <p style="margin:0 0 24px;font-size:16px;color:#222;line-height:1.6;">
                                <strong>%s 님, 환영합니다!</strong>
                              </p>
                              <p style="margin:0 0 32px;font-size:15px;color:#444;line-height:1.8;">
                                Attune 회원가입을 완료하려면 아래 버튼을 클릭하여 이메일 인증을 완료해주세요.
                              </p>

                              <!-- 버튼 -->
                              <table width="100%%" cellpadding="0" cellspacing="0">
                                <tr>
                                  <td align="center" style="padding:8px 0 32px;">
                                    <a href="%s"
                                       style="display:inline-block;background:#1a1a2e;color:#ffffff;font-size:15px;font-weight:600;
                                              text-decoration:none;padding:14px 40px;border-radius:6px;">
                                      이메일 인증하기
                                    </a>
                                  </td>
                                </tr>
                              </table>

                              <p style="margin:0 0 4px;font-size:13px;color:#888;line-height:1.8;">
                                버튼이 동작하지 않는 경우 아래 링크를 복사하여 브라우저에 붙여넣으세요.
                              </p>
                              <p style="margin:0;font-size:12px;color:#aaa;word-break:break-all;">
                                <a href="%s" style="color:#aaa;">%s</a>
                              </p>

                              <p style="margin:28px 0 0;font-size:13px;color:#888;line-height:1.8;">
                                · 링크는 발송 후 <strong>24시간</strong> 동안 유효합니다.<br>
                                · 본인이 가입하지 않으셨다면 이 메일을 무시하세요.<br>
                                · 추가 문의사항은 Attune 고객센터로 문의해주세요.
                              </p>
                            </td>
                          </tr>

                          <!-- 구분선 -->
                          <tr>
                            <td style="padding:0 40px;">
                              <hr style="border:none;border-top:1px solid #eeeeee;margin:0;">
                            </td>
                          </tr>

                          <!-- 푸터 -->
                          <tr>
                            <td style="padding:24px 40px;background:#fafafa;">
                              <p style="margin:0;font-size:11px;color:#aaa;line-height:1.8;">
                                이 메일은 Attune 서비스 가입을 위해 발송된 메일로, 수신 동의 여부와 무관하게 발송되었습니다.<br>
                                이 메일은 발신 전용으로 회신하실 수 없습니다.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(nickname, verificationLink, verificationLink, verificationLink);
    }

    private String buildWelcomeHtml(String nickname) {
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background:#f5f5f5;font-family:'Apple SD Gothic Neo',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr>
                      <td align="center" style="padding:40px 0;">
                        <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:8px;overflow:hidden;">

                          <!-- 헤더 -->
                          <tr>
                            <td style="background:#1a1a2e;padding:28px 40px;">
                              <span style="color:#ffffff;font-size:22px;font-weight:700;letter-spacing:-0.5px;">Attune</span>
                            </td>
                          </tr>

                          <!-- 본문 -->
                          <tr>
                            <td style="padding:44px 40px 32px;">
                              <p style="margin:0 0 24px;font-size:16px;color:#222;line-height:1.6;">
                                <strong>%s 님, 환영합니다!</strong>
                              </p>
                              <p style="margin:0 0 32px;font-size:15px;color:#444;line-height:1.8;">
                                Attune 회원이 되신 것을 진심으로 환영합니다.<br>
                                지금 바로 서비스를 시작해보세요.
                              </p>

                              <p style="margin:28px 0 0;font-size:13px;color:#888;line-height:1.8;">
                                · 가입하신 서비스 이용 중 궁금한 점이 있으시면 고객센터로 문의해주세요.<br>
                                · 본인이 가입하지 않으셨다면 Attune 고객센터로 문의해주세요.
                              </p>
                            </td>
                          </tr>

                          <!-- 구분선 -->
                          <tr>
                            <td style="padding:0 40px;">
                              <hr style="border:none;border-top:1px solid #eeeeee;margin:0;">
                            </td>
                          </tr>

                          <!-- 푸터 -->
                          <tr>
                            <td style="padding:24px 40px;background:#fafafa;">
                              <p style="margin:0;font-size:11px;color:#aaa;line-height:1.8;">
                                이 메일은 Attune 서비스 가입을 위해 발송된 메일로, 수신 동의 여부와 무관하게 발송되었습니다.<br>
                                이 메일은 발신 전용으로 회신하실 수 없습니다.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(nickname);
    }

    private String buildPasswordResetHtml(String nickname, String resetLink) {
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background:#f5f5f5;font-family:'Apple SD Gothic Neo',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr>
                      <td align="center" style="padding:40px 0;">
                        <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:8px;overflow:hidden;">

                          <!-- 헤더 -->
                          <tr>
                            <td style="background:#1a1a2e;padding:28px 40px;">
                              <span style="color:#ffffff;font-size:22px;font-weight:700;letter-spacing:-0.5px;">Attune</span>
                            </td>
                          </tr>

                          <!-- 본문 -->
                          <tr>
                            <td style="padding:44px 40px 32px;">
                              <p style="margin:0 0 24px;font-size:16px;color:#222;line-height:1.6;">
                                <strong>%s 님, 안녕하세요.</strong>
                              </p>
                              <p style="margin:0 0 32px;font-size:15px;color:#444;line-height:1.8;">
                                비밀번호 재설정을 요청하셨습니다.<br>
                                아래 버튼을 클릭하여 새 비밀번호를 설정하세요.
                              </p>

                              <!-- 버튼 -->
                              <table width="100%%" cellpadding="0" cellspacing="0">
                                <tr>
                                  <td align="center" style="padding:8px 0 32px;">
                                    <a href="%s"
                                       style="display:inline-block;background:#1a1a2e;color:#ffffff;font-size:15px;font-weight:600;
                                              text-decoration:none;padding:14px 40px;border-radius:6px;">
                                      비밀번호 재설정하기
                                    </a>
                                  </td>
                                </tr>
                              </table>

                              <p style="margin:0 0 4px;font-size:13px;color:#888;line-height:1.8;">
                                버튼이 동작하지 않는 경우 아래 링크를 복사하여 브라우저에 붙여넣으세요.
                              </p>
                              <p style="margin:0;font-size:12px;color:#aaa;word-break:break-all;">
                                <a href="%s" style="color:#aaa;">%s</a>
                              </p>

                              <p style="margin:28px 0 0;font-size:13px;color:#888;line-height:1.8;">
                                · 링크는 발송 후 <strong>24시간</strong> 동안 유효합니다.<br>
                                · 본인이 요청하지 않으셨다면 이 메일을 무시하세요.<br>
                                · 추가 문의사항은 Attune 고객센터로 문의해주세요.
                              </p>
                            </td>
                          </tr>

                          <!-- 구분선 -->
                          <tr>
                            <td style="padding:0 40px;">
                              <hr style="border:none;border-top:1px solid #eeeeee;margin:0;">
                            </td>
                          </tr>

                          <!-- 푸터 -->
                          <tr>
                            <td style="padding:24px 40px;background:#fafafa;">
                              <p style="margin:0;font-size:11px;color:#aaa;line-height:1.8;">
                                이 메일은 Attune 서비스 이용을 위해 발송된 메일로, 수신 동의 여부와 무관하게 발송되었습니다.<br>
                                이 메일은 발신 전용으로 회신하실 수 없습니다.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(nickname, resetLink, resetLink, resetLink);
    }


}
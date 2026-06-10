package kz.whosnext.hr.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    @Async
    public void sendConfirmationEmail(String to, String token) {
        String link = baseUrl + "/api/v1/auth/confirm?token=" + token;
        String subject = "HR-Platform — Подтверждение email";
        String body = """
                <html><body>
                <h2>Добро пожаловать в HR-Platform!</h2>
                <p>Для подтверждения вашего email перейдите по ссылке:</p>
                <p><a href="%s">Подтвердить email</a></p>
                <p>Ссылка действительна 24 часа.</p>
                <br><p>С уважением, команда HR-Platform</p>
                </body></html>
                """.formatted(link);
        sendHtml(to, subject, body);
    }

    @Async
    public void sendPasswordResetEmail(String to, String token) {
        String link = baseUrl + "/api/v1/auth/reset-password?token=" + token;
        String subject = "HR-Platform — Сброс пароля";
        String body = """
                <html><body>
                <h2>Сброс пароля</h2>
                <p>Вы запросили сброс пароля. Перейдите по ссылке:</p>
                <p><a href="%s">Сбросить пароль</a></p>
                <p>Ссылка действительна 1 час. Если вы не запрашивали сброс — проигнорируйте это письмо.</p>
                <br><p>С уважением, команда HR-Platform</p>
                </body></html>
                """.formatted(link);
        sendHtml(to, subject, body);
    }

    @Async
    public void sendPasswordChangedNotification(String to) {
        String subject = "HR-Platform — Пароль изменён";
        String body = """
                <html><body>
                <h2>Пароль изменён</h2>
                <p>Ваш пароль был успешно изменён. Если это были не вы — немедленно свяжитесь с поддержкой.</p>
                <br><p>С уважением, команда HR-Platform</p>
                </body></html>
                """;
        sendHtml(to, subject, body);
    }

    @Async
    public void sendAccountDeletedNotification(String to) {
        String subject = "HR-Platform — Аккаунт удалён";
        String body = """
                <html><body>
                <h2>Аккаунт удалён</h2>
                <p>Ваш аккаунт в HR-Platform был удалён администратором.</p>
                <p>Если у вас есть вопросы — обратитесь в поддержку.</p>
                <br><p>С уважением, команда HR-Platform</p>
                </body></html>
                """;
        sendHtml(to, subject, body);
    }

    private void sendHtml(String to, String subject, String htmlBody) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email отправлен: to={}, subject={}", to, subject);
        } catch (MailException | jakarta.mail.MessagingException e) {
            log.error("Ошибка отправки email: to={}, subject={}, error={}", to, subject, e.getMessage());
        }
    }
}


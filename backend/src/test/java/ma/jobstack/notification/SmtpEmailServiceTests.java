package ma.jobstack.notification;

import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SmtpEmailServiceTests {

    @Test
    void sendAsync_sendsMessageWithExpectedFields() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmtpEmailService service = new SmtpEmailService(mailSender, "no-reply@jobstack.ma");

        service.sendAsync("candidate@jobstack.ma", "Subject line", "Body text");

        var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(sent.getFrom()).isEqualTo("no-reply@jobstack.ma");
        org.assertj.core.api.Assertions.assertThat(sent.getTo()).containsExactly("candidate@jobstack.ma");
        org.assertj.core.api.Assertions.assertThat(sent.getSubject()).isEqualTo("Subject line");
        org.assertj.core.api.Assertions.assertThat(sent.getText()).isEqualTo("Body text");
    }

    @Test
    void sendAsync_whenMailServerFails_logsInsteadOfThrowing() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));
        SmtpEmailService service = new SmtpEmailService(mailSender, "no-reply@jobstack.ma");

        assertThatCode(() -> service.sendAsync("candidate@jobstack.ma", "Subject", "Body")).doesNotThrowAnyException();
    }
}

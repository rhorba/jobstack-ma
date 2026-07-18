package ma.jobstack.notification;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class NotificationService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneOffset.UTC);

    private final EmailService emailService;

    public NotificationService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void sendWelcomeEmail(String to) {
        emailService.sendAsync(to, "Welcome to JobStack.ma",
                "Thanks for registering on JobStack.ma. You can now sign in and get started.");
    }

    public void sendApplicationSubmitted(String to, String jobTitle) {
        emailService.sendAsync(to, "Application submitted: " + jobTitle,
                "Your application for \"" + jobTitle + "\" has been submitted. The employer will review it soon.");
    }

    public void sendPostingRejected(String to, String jobTitle, String reason) {
        emailService.sendAsync(to, "Your job posting was rejected: " + jobTitle,
                "Your posting \"" + jobTitle + "\" was rejected by an administrator.\nReason: " + reason);
    }

    public void sendPostingExpirySoon(String to, String jobTitle, Instant expiresAt) {
        emailService.sendAsync(to, "Your job posting expires soon: " + jobTitle,
                "Your posting \"" + jobTitle + "\" will expire on " + DATE_FORMAT.format(expiresAt)
                        + ". Post a new listing to keep hiring.");
    }
}

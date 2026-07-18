package ma.jobstack.notification;

public interface EmailService {
    void sendAsync(String to, String subject, String body);
}

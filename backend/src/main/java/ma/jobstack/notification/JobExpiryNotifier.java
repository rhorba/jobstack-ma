package ma.jobstack.notification;

import ma.jobstack.employer.CompanyRepository;
import ma.jobstack.auth.UserRepository;
import ma.jobstack.job.JobPosting;
import ma.jobstack.job.JobPostingRepository;
import ma.jobstack.job.JobStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class JobExpiryNotifier {

    private final JobPostingRepository jobPostingRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final int daysBefore;

    public JobExpiryNotifier(JobPostingRepository jobPostingRepository, CompanyRepository companyRepository,
                              UserRepository userRepository, NotificationService notificationService,
                              @Value("${job.expiry-notice.days-before}") int daysBefore) {
        this.jobPostingRepository = jobPostingRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.daysBefore = daysBefore;
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void notifyExpiringSoon() {
        Instant now = Instant.now();
        Instant threshold = now.plus(Duration.ofDays(daysBefore));
        List<JobPosting> expiringSoon = jobPostingRepository
                .findByStatusAndExpiresAtBetweenAndExpiryNoticeSentAtIsNull(JobStatus.LIVE, now, threshold);

        for (JobPosting posting : expiringSoon) {
            companyRepository.findById(posting.getCompanyId())
                    .flatMap(company -> userRepository.findById(company.getOwnerUserId()))
                    .ifPresent(user -> notificationService.sendPostingExpirySoon(
                            user.getEmail(), posting.getTitle(), posting.getExpiresAt()));
            posting.markExpiryNoticeSent(now);
            jobPostingRepository.save(posting);
        }
    }
}

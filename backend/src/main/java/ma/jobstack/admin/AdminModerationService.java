package ma.jobstack.admin;

import ma.jobstack.auth.UserRepository;
import ma.jobstack.employer.CompanyRepository;
import ma.jobstack.job.JobPosting;
import ma.jobstack.job.JobPostingRepository;
import ma.jobstack.job.JobStatus;
import ma.jobstack.notification.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class AdminModerationService {

    private static final List<JobStatus> QUEUE_STATUSES = List.of(JobStatus.PENDING_PAYMENT, JobStatus.LIVE);

    private final JobPostingRepository jobPostingRepository;
    private final ModerationActionRepository moderationActionRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AdminModerationService(JobPostingRepository jobPostingRepository,
                                   ModerationActionRepository moderationActionRepository,
                                   CompanyRepository companyRepository, UserRepository userRepository,
                                   NotificationService notificationService) {
        this.jobPostingRepository = jobPostingRepository;
        this.moderationActionRepository = moderationActionRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public List<JobPosting> listQueue() {
        return jobPostingRepository.findByStatusIn(QUEUE_STATUSES);
    }

    @Transactional
    public JobPosting moderate(UUID jobPostingId, UUID adminUserId, ModerationActionType action, String reason) {
        JobPosting posting = jobPostingRepository.findById(jobPostingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!QUEUE_STATUSES.contains(posting.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Posting is not in a moderatable state");
        }

        switch (action) {
            case REJECT -> {
                if (reason == null || reason.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reason is required to reject a posting");
                }
                posting.reject(reason);
                notifyOwner(posting, reason);
            }
            case REMOVE -> posting.remove();
            case APPROVE -> {
                // no status change — approval just clears the posting from further review
            }
        }

        jobPostingRepository.save(posting);
        moderationActionRepository.save(new ModerationAction(jobPostingId, adminUserId, action, reason));
        return posting;
    }

    private void notifyOwner(JobPosting posting, String reason) {
        companyRepository.findById(posting.getCompanyId())
                .flatMap(company -> userRepository.findById(company.getOwnerUserId()))
                .ifPresent(user -> notificationService.sendPostingRejected(user.getEmail(), posting.getTitle(), reason));
    }
}

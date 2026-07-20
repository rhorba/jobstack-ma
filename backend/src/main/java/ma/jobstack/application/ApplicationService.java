package ma.jobstack.application;

import ma.jobstack.analytics.AnalyticsService;
import ma.jobstack.auth.User;
import ma.jobstack.auth.UserRepository;
import ma.jobstack.candidate.CandidateProfile;
import ma.jobstack.candidate.CandidateProfileRepository;
import ma.jobstack.candidate.CvStorageService;
import ma.jobstack.employer.Company;
import ma.jobstack.employer.CompanyRepository;
import ma.jobstack.job.JobPosting;
import ma.jobstack.job.JobPostingRepository;
import ma.jobstack.job.JobStatus;
import ma.jobstack.notification.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Service
public class ApplicationService {

    private final JobPostingRepository jobPostingRepository;
    private final CompanyRepository companyRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final CvStorageService cvStorageService;
    private final NotificationService notificationService;
    private final AnalyticsService analyticsService;

    public ApplicationService(JobPostingRepository jobPostingRepository, CompanyRepository companyRepository,
                               CandidateProfileRepository candidateProfileRepository, UserRepository userRepository,
                               ApplicationRepository applicationRepository, CvStorageService cvStorageService,
                               NotificationService notificationService, AnalyticsService analyticsService) {
        this.jobPostingRepository = jobPostingRepository;
        this.companyRepository = companyRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.cvStorageService = cvStorageService;
        this.notificationService = notificationService;
        this.analyticsService = analyticsService;
    }

    @Transactional
    public Application apply(UUID jobPostingId, UUID candidateUserId) {
        JobPosting posting = jobPostingRepository.findByIdAndStatus(jobPostingId, JobStatus.LIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        CandidateProfile profile = candidateProfileRepository.findByUserId(candidateUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!isComplete(profile)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Complete your profile and upload a CV before you can apply");
        }

        if (applicationRepository.existsByJobPostingIdAndCandidateProfileId(jobPostingId, profile.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already applied to this job");
        }

        Application application = new Application(posting.getId(), profile.getId());
        applicationRepository.save(application);

        userRepository.findById(candidateUserId)
                .ifPresent(user -> notificationService.sendApplicationSubmitted(user.getEmail(), posting.getTitle()));
        analyticsService.track("application_submitted", candidateUserId,
                Map.of("job_posting_id", posting.getId().toString()));
        return application;
    }

    public Page<Application> listApplicants(UUID jobPostingId, UUID employerUserId, Pageable pageable) {
        JobPosting posting = jobPostingRepository.findById(jobPostingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertOwnedByEmployer(posting, employerUserId);
        return applicationRepository.findByJobPostingId(jobPostingId, pageable);
    }

    public byte[] downloadApplicantCv(UUID jobPostingId, UUID candidateProfileId, UUID employerUserId) {
        JobPosting posting = jobPostingRepository.findById(jobPostingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertOwnedByEmployer(posting, employerUserId);

        if (!applicationRepository.existsByJobPostingIdAndCandidateProfileId(jobPostingId, candidateProfileId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        CandidateProfile profile = candidateProfileRepository.findById(candidateProfileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return cvStorageService.load(profile.getUserId());
    }

    public CandidateProfile findProfile(UUID candidateProfileId) {
        return candidateProfileRepository.findById(candidateProfileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private boolean isComplete(CandidateProfile profile) {
        return profile.getFullName() != null && !profile.getFullName().isBlank()
                && profile.getPhone() != null && !profile.getPhone().isBlank()
                && profile.getSector() != null && !profile.getSector().isBlank()
                && profile.getCity() != null && !profile.getCity().isBlank()
                && profile.getCvPath() != null;
    }

    private void assertOwnedByEmployer(JobPosting posting, UUID employerUserId) {
        Company company = companyRepository.findById(posting.getCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!company.getOwnerUserId().equals(employerUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}

package ma.jobstack.employer;

import jakarta.validation.Valid;
import ma.jobstack.application.Application;
import ma.jobstack.application.ApplicationService;
import ma.jobstack.application.dto.ApplicantResponse;
import ma.jobstack.candidate.CandidateProfile;
import ma.jobstack.auth.User;
import ma.jobstack.auth.UserRepository;
import ma.jobstack.employer.dto.CompanyResponse;
import ma.jobstack.employer.dto.CreateCompanyRequest;
import ma.jobstack.shared.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employers")
public class EmployerController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final ApplicationService applicationService;

    public EmployerController(UserRepository userRepository, CompanyRepository companyRepository,
                               ApplicationService applicationService) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.applicationService = applicationService;
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal UUID userId) {
        User user = findUser(userId);
        return ResponseEntity.ok(new MeResponse(user.getEmail(), user.getRole().name()));
    }

    @PostMapping("/me/company")
    public ResponseEntity<CompanyResponse> createCompany(@AuthenticationPrincipal UUID userId,
                                                           @Valid @RequestBody CreateCompanyRequest request) {
        findUser(userId);
        if (companyRepository.existsByOwnerUserId(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Company already registered for this employer");
        }
        Company company = new Company(userId, request.name(), request.sector(), request.city());
        companyRepository.save(company);
        return ResponseEntity.ok(toResponse(company));
    }

    @GetMapping("/me/company")
    public ResponseEntity<CompanyResponse> getCompany(@AuthenticationPrincipal UUID userId) {
        Company company = companyRepository.findByOwnerUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(toResponse(company));
    }

    @GetMapping("/me/jobs/{jobId}/applicants")
    public ResponseEntity<PageResponse<ApplicantResponse>> listApplicants(@AuthenticationPrincipal UUID userId,
                                                                     @PathVariable UUID jobId,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        Page<Application> applications = applicationService.listApplicants(jobId, userId, pageable);
        List<ApplicantResponse> content = applications.getContent().stream().map(a -> {
            CandidateProfile profile = applicationService.findProfile(a.getCandidateProfileId());
            User candidateUser = applicationService.findUser(profile.getUserId());
            String cvUrl = "/api/v1/employers/me/jobs/" + jobId + "/applicants/" + profile.getId() + "/cv";
            return new ApplicantResponse(a.getId().toString(), profile.getFullName(), candidateUser.getEmail(),
                    profile.getPhone(), cvUrl);
        }).toList();
        return ResponseEntity.ok(new PageResponse<>(content, applications.getNumber(), applications.getSize(),
                applications.getTotalElements(), applications.getTotalPages()));
    }

    private static int clampSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    @GetMapping("/me/jobs/{jobId}/applicants/{candidateProfileId}/cv")
    public ResponseEntity<byte[]> downloadApplicantCv(@AuthenticationPrincipal UUID userId,
                                                        @PathVariable UUID jobId,
                                                        @PathVariable UUID candidateProfileId) {
        byte[] content = applicationService.downloadApplicantCv(jobId, candidateProfileId, userId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(content);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private CompanyResponse toResponse(Company company) {
        return new CompanyResponse(company.getId().toString(), company.getName(), company.getSector(),
                company.getCity(), company.isVerified());
    }

    public record MeResponse(String email, String role) {
    }
}

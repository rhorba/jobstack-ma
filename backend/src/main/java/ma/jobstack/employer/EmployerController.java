package ma.jobstack.employer;

import jakarta.validation.Valid;
import ma.jobstack.auth.User;
import ma.jobstack.auth.UserRepository;
import ma.jobstack.employer.dto.CompanyResponse;
import ma.jobstack.employer.dto.CreateCompanyRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employers")
public class EmployerController {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    public EmployerController(UserRepository userRepository, CompanyRepository companyRepository) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
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

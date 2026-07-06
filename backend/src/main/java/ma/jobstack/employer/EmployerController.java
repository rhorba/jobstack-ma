package ma.jobstack.employer;

import ma.jobstack.auth.User;
import ma.jobstack.auth.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Placeholder for the employer/company domain (full CRUD lands in Sprint 4).
 * Exists now to prove role-based authorization end-to-end (Story 2.4).
 */
@RestController
@RequestMapping("/api/v1/employers")
public class EmployerController {

    private final UserRepository userRepository;

    public EmployerController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(new MeResponse(user.getEmail(), user.getRole().name()));
    }

    public record MeResponse(String email, String role) {
    }
}

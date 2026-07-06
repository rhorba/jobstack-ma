package ma.jobstack.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Placeholder for the admin moderation/metrics domain (full functionality lands in Sprint 7).
 * Exists now to prove role-based authorization end-to-end (Story 2.4).
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("ok");
    }
}

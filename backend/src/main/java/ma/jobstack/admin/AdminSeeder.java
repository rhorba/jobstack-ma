package ma.jobstack.admin;

import ma.jobstack.auth.User;
import ma.jobstack.auth.UserRepository;
import ma.jobstack.auth.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedEmail;
    private final String seedPassword;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        @Value("${admin.seed.email}") String seedEmail,
                        @Value("${admin.seed.password}") String seedPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedEmail = seedEmail;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.countByRole(UserRole.ADMIN) > 0) {
            return;
        }
        if (seedEmail == null || seedEmail.isBlank() || seedPassword == null || seedPassword.isBlank()) {
            log.warn("No ADMIN account exists and ADMIN_SEED_EMAIL/ADMIN_SEED_PASSWORD are not set — skipping seed");
            return;
        }
        User admin = new User(seedEmail, passwordEncoder.encode(seedPassword), UserRole.ADMIN);
        userRepository.save(admin);
        log.info("Seeded initial ADMIN account: {}", seedEmail);
    }
}

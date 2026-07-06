package ma.jobstack.auth;

import ma.jobstack.auth.dto.AuthResponse;
import ma.jobstack.auth.dto.LoginRequest;
import ma.jobstack.auth.dto.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (request.role() == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot self-register as ADMIN");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = new User(request.email(), passwordEncoder.encode(request.password()), request.role());
        userRepository.save(user);
    }

    public record LoginResult(AuthResponse response, String rawRefreshToken) {
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        return issueTokens(user);
    }

    @Transactional
    public LoginResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing refresh token");
        }
        String hash = jwtService.hashRefreshToken(rawRefreshToken);
        User user = userRepository.findByRefreshTokenHash(hash)
                .filter(u -> u.getRefreshTokenExpiresAt() != null && u.getRefreshTokenExpiresAt().isAfter(Instant.now()))
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token"));

        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String hash = jwtService.hashRefreshToken(rawRefreshToken);
        userRepository.findByRefreshTokenHash(hash).ifPresent(user -> {
            user.clearRefreshToken();
            userRepository.save(user);
        });
    }

    private LoginResult issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = jwtService.generateOpaqueRefreshToken();
        user.setRefreshToken(jwtService.hashRefreshToken(rawRefreshToken), Instant.now().plus(JwtService.REFRESH_TOKEN_TTL));
        userRepository.save(user);

        AuthResponse response = new AuthResponse(accessToken, JwtService.ACCESS_TOKEN_TTL.toSeconds(), user.getRole().name());
        return new LoginResult(response, rawRefreshToken);
    }
}

package ma.jobstack.auth.dto;

public record AuthResponse(
        String accessToken,
        long expiresInSeconds,
        String role
) {
}

package ma.jobstack.admin.dto;

public record UserStatusResponse(
        String id,
        String email,
        String status
) {
}

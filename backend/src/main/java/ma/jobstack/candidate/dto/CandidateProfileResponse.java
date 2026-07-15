package ma.jobstack.candidate.dto;

public record CandidateProfileResponse(
        String email,
        String role,
        String fullName,
        String phone,
        String sector,
        String city,
        boolean hasCv
) {
}

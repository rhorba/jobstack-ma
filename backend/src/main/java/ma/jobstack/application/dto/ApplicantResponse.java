package ma.jobstack.application.dto;

public record ApplicantResponse(String applicationId, String fullName, String email, String phone,
                                 String cvDownloadUrl) {
}

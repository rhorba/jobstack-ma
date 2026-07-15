package ma.jobstack.employer.dto;

public record CompanyResponse(
        String id,
        String name,
        String sector,
        String city,
        boolean verified
) {
}

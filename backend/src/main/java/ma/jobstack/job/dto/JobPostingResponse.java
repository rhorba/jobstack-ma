package ma.jobstack.job.dto;

public record JobPostingResponse(
        String id,
        String title,
        String description,
        String sector,
        String city,
        String contractType,
        String status,
        String companyName
) {
}

package ma.jobstack.admin.dto;

import java.math.BigDecimal;

public record AdminMetricsResponse(
        long totalPostings,
        long liveJobPostings,
        long totalApplications,
        long confirmedPayments,
        BigDecimal confirmedRevenueMad
) {
}

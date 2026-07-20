package ma.jobstack.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.jobstack.TestcontainersConfiguration;
import ma.jobstack.employer.Company;
import ma.jobstack.employer.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class JobSearchTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JobPostingRepository jobPostingRepository;

    private String accessTokenForNewEmployer() throws Exception {
        String email = "employer-" + UUID.randomUUID() + "@jobstack.ma";
        String password = "supersecret123";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password, "role", "EMPLOYER"))))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private UUID companyForNewEmployer() throws Exception {
        String token = accessTokenForNewEmployer();
        MvcResult result = mockMvc.perform(post("/api/v1/employers/me/company")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Test Co " + UUID.randomUUID(), "sector", "automotive", "city", "Tangier"))))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private JobPosting liveJob(UUID companyId, String title, String sector, String city, String contractType) {
        JobPosting posting = new JobPosting(companyId, title, "desc", sector, city, contractType);
        posting.setStatus(JobStatus.LIVE);
        return jobPostingRepository.save(posting);
    }

    private JobPosting draftJob(UUID companyId, String title) {
        return jobPostingRepository.save(new JobPosting(companyId, title, "desc", "automotive", "Tangier", "CDI"));
    }

    @Test
    void search_withNoFilters_returnsOnlyLivePostings() throws Exception {
        UUID companyId = companyForNewEmployer();
        String uniqueTitle = "LiveJob-" + UUID.randomUUID();
        String draftTitle = "DraftJob-" + UUID.randomUUID();
        liveJob(companyId, uniqueTitle, "automotive", "Tangier", "CDI");
        draftJob(companyId, draftTitle);

        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.title=='" + uniqueTitle + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.title=='" + draftTitle + "')]").doesNotExist());
    }

    @Test
    void search_filtersBySectorCityContractType() throws Exception {
        UUID companyId = companyForNewEmployer();
        String matchTitle = "Match-" + UUID.randomUUID();
        String noMatchTitle = "NoMatch-" + UUID.randomUUID();
        liveJob(companyId, matchTitle, "pharma", "Casablanca", "CDD");
        liveJob(companyId, noMatchTitle, "automotive", "Tangier", "CDI");

        mockMvc.perform(get("/api/v1/jobs")
                        .param("sector", "pharma")
                        .param("city", "Casablanca")
                        .param("contractType", "CDD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.title=='" + matchTitle + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.title=='" + noMatchTitle + "')]").doesNotExist());
    }

    @Test
    void search_withSqlInjectionAttemptInSectorFilter_isSafelyParameterizedAndReturnsNoMatches() throws Exception {
        mockMvc.perform(get("/api/v1/jobs").param("sector", "automotive'; DROP TABLE job_postings; --"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // table still intact and queryable afterwards
        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isOk());
    }

    @Test
    void search_withSqlInjectionAttemptInCityFilter_isSafelyParameterizedAndReturnsNoMatches() throws Exception {
        mockMvc.perform(get("/api/v1/jobs").param("city", "Tangier' OR '1'='1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isOk());
    }

    @Test
    void search_withSqlInjectionAttemptInContractTypeFilter_isSafelyParameterizedAndReturnsNoMatches() throws Exception {
        mockMvc.perform(get("/api/v1/jobs").param("contractType", "CDI'; DROP TABLE job_postings; --"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isOk());
    }

    @Test
    void search_withNegativePage_gracefullyClampsToFirstPage() throws Exception {
        UUID companyId = companyForNewEmployer();
        String uniqueTitle = "NegPage-" + UUID.randomUUID();
        liveJob(companyId, uniqueTitle, "automotive", "Tangier", "CDI");

        mockMvc.perform(get("/api/v1/jobs").param("page", "-5").param("sector", "automotive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.content[?(@.title=='" + uniqueTitle + "')]").exists());
    }

    @Test
    void search_withHugePageSize_isClampedToMax() throws Exception {
        mockMvc.perform(get("/api/v1/jobs").param("size", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void search_withZeroOrNegativeSize_fallsBackToDefault() throws Exception {
        mockMvc.perform(get("/api/v1/jobs").param("size", "-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void getById_liveJob_returns200() throws Exception {
        UUID companyId = companyForNewEmployer();
        JobPosting posting = liveJob(companyId, "Detail-" + UUID.randomUUID(), "automotive", "Tangier", "CDI");

        mockMvc.perform(get("/api/v1/jobs/" + posting.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIVE"));
    }

    @Test
    void getById_draftJob_returns404_notPubliclyVisible() throws Exception {
        UUID companyId = companyForNewEmployer();
        JobPosting posting = draftJob(companyId, "Hidden-" + UUID.randomUUID());

        mockMvc.perform(get("/api/v1/jobs/" + posting.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}

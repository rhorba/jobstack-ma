package ma.jobstack.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.jobstack.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "cv.storage.path=target/test-cvs")
class ApplicationFlowTests {

    private static final byte[] VALID_PDF_BYTES = "%PDF-1.4 fake but valid header".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessTokenForNewUser(String role) throws Exception {
        String email = role.toLowerCase() + "-" + UUID.randomUUID() + "@jobstack.ma";
        String password = "supersecret123";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password, "role", role))))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private void completeCandidateProfile(String candidateToken) throws Exception {
        mockMvc.perform(put("/api/v1/candidates/me")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Amine Test", "phone", "0600000000", "sector", "IT", "city", "Rabat"))))
                .andExpect(status().isOk());

        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", VALID_PDF_BYTES);
        mockMvc.perform(multipart("/api/v1/candidates/me/cv").file(file)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());
    }

    private String draftJobFor(String employerToken) throws Exception {
        mockMvc.perform(post("/api/v1/employers/me/company")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Atlas Automotive SARL", "sector", "automotive", "city", "Tangier"))))
                .andExpect(status().isOk());

        MvcResult jobResult = mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Automotive QA Engineer",
                                "description", "Inspect assembly line output for defects.",
                                "sector", "automotive",
                                "city", "Tangier",
                                "contractType", "CDI"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(jobResult.getResponse().getContentAsString()).get("id").asText();
    }

    private String liveJobFor(String employerToken) throws Exception {
        String jobId = draftJobFor(employerToken);

        MvcResult checkoutResult = mockMvc.perform(post("/api/v1/jobs/" + jobId + "/checkout")
                        .header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isOk())
                .andReturn();
        String paymentId = objectMapper.readTree(checkoutResult.getResponse().getContentAsString())
                .get("paymentId").asText();

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/mock-outcome")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("outcome", "SUCCESS"))))
                .andExpect(status().isOk());

        return jobId;
    }

    @Test
    void apply_asCompleteCandidateToLiveJob_createsApplication() throws Exception {
        String employerToken = accessTokenForNewUser("EMPLOYER");
        String jobId = liveJobFor(employerToken);
        String candidateToken = accessTokenForNewUser("CANDIDATE");
        completeCandidateProfile(candidateToken);

        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/apply").header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void apply_withoutCv_isBlockedWithClearMessage() throws Exception {
        String employerToken = accessTokenForNewUser("EMPLOYER");
        String jobId = liveJobFor(employerToken);
        String candidateToken = accessTokenForNewUser("CANDIDATE");
        // profile fields set, but no CV uploaded
        mockMvc.perform(put("/api/v1/candidates/me")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Amine Test", "phone", "0600000000", "sector", "IT", "city", "Rabat"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/apply").header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isConflict());
    }

    @Test
    void apply_duplicateApply_isBlocked() throws Exception {
        String employerToken = accessTokenForNewUser("EMPLOYER");
        String jobId = liveJobFor(employerToken);
        String candidateToken = accessTokenForNewUser("CANDIDATE");
        completeCandidateProfile(candidateToken);

        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/apply").header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/apply").header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isConflict());
    }

    @Test
    void apply_toNonLiveJob_returns404() throws Exception {
        String employerToken = accessTokenForNewUser("EMPLOYER");
        String jobId = draftJobFor(employerToken); // still DRAFT, never paid
        String candidateToken = accessTokenForNewUser("CANDIDATE");
        completeCandidateProfile(candidateToken);

        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/apply").header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void apply_asEmployer_isForbidden() throws Exception {
        String employerToken = accessTokenForNewUser("EMPLOYER");
        String jobId = liveJobFor(employerToken);

        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/apply").header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void listApplicants_byOwningEmployer_returnsApplicantDetails() throws Exception {
        String employerToken = accessTokenForNewUser("EMPLOYER");
        String jobId = liveJobFor(employerToken);
        String candidateToken = accessTokenForNewUser("CANDIDATE");
        completeCandidateProfile(candidateToken);
        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/apply").header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/employers/me/jobs/" + jobId + "/applicants")
                        .header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fullName").value("Amine Test"))
                .andExpect(jsonPath("$.content[0].phone").value("0600000000"))
                .andExpect(jsonPath("$.content[0].email").isNotEmpty())
                .andExpect(jsonPath("$.content[0].cvDownloadUrl").value(
                        org.hamcrest.Matchers.startsWith("/api/v1/employers/me/jobs/" + jobId + "/applicants/")))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listApplicants_byNonOwningEmployer_returns403() throws Exception {
        String ownerToken = accessTokenForNewUser("EMPLOYER");
        String jobId = liveJobFor(ownerToken);
        String otherToken = accessTokenForNewUser("EMPLOYER");

        mockMvc.perform(get("/api/v1/employers/me/jobs/" + jobId + "/applicants")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void listApplicants_withNoApplicants_returnsEmptyList() throws Exception {
        String employerToken = accessTokenForNewUser("EMPLOYER");
        String jobId = liveJobFor(employerToken);

        mockMvc.perform(get("/api/v1/employers/me/jobs/" + jobId + "/applicants")
                        .header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void listApplicants_withNegativePageOrHugeSize_handledGracefully() throws Exception {
        String employerToken = accessTokenForNewUser("EMPLOYER");
        String jobId = liveJobFor(employerToken);

        mockMvc.perform(get("/api/v1/employers/me/jobs/" + jobId + "/applicants")
                        .header("Authorization", "Bearer " + employerToken)
                        .param("page", "-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0));

        mockMvc.perform(get("/api/v1/employers/me/jobs/" + jobId + "/applicants")
                        .header("Authorization", "Bearer " + employerToken)
                        .param("size", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void downloadApplicantCv_byOwningEmployer_returnsCvBytes() throws Exception {
        String employerToken = accessTokenForNewUser("EMPLOYER");
        String jobId = liveJobFor(employerToken);
        String candidateToken = accessTokenForNewUser("CANDIDATE");
        completeCandidateProfile(candidateToken);
        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/apply").header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());

        MvcResult listResult = mockMvc.perform(get("/api/v1/employers/me/jobs/" + jobId + "/applicants")
                        .header("Authorization", "Bearer " + employerToken))
                .andReturn();
        String cvUrl = objectMapper.readTree(listResult.getResponse().getContentAsString()).get(0).get("cvDownloadUrl").asText();

        mockMvc.perform(get(cvUrl).header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isOk())
                .andExpect(content().bytes(VALID_PDF_BYTES));
    }

    @Test
    void downloadApplicantCv_byNonOwningEmployer_returns403() throws Exception {
        String ownerToken = accessTokenForNewUser("EMPLOYER");
        String jobId = liveJobFor(ownerToken);
        String candidateToken = accessTokenForNewUser("CANDIDATE");
        completeCandidateProfile(candidateToken);
        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/apply").header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());

        MvcResult listResult = mockMvc.perform(get("/api/v1/employers/me/jobs/" + jobId + "/applicants")
                        .header("Authorization", "Bearer " + ownerToken))
                .andReturn();
        String cvUrl = objectMapper.readTree(listResult.getResponse().getContentAsString()).get(0).get("cvDownloadUrl").asText();

        String otherEmployerToken = accessTokenForNewUser("EMPLOYER");
        mockMvc.perform(get(cvUrl).header("Authorization", "Bearer " + otherEmployerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void downloadApplicantCv_forCandidateWhoNeverApplied_returns404() throws Exception {
        String employerToken = accessTokenForNewUser("EMPLOYER");
        String jobId = liveJobFor(employerToken);

        mockMvc.perform(get("/api/v1/employers/me/jobs/" + jobId + "/applicants/" + UUID.randomUUID() + "/cv")
                        .header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isNotFound());
    }
}

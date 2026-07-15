package ma.jobstack.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.jobstack.TestcontainersConfiguration;
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
class JobPostingTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private String accessTokenForNewCandidate() throws Exception {
        String email = "candidate-" + UUID.randomUUID() + "@jobstack.ma";
        String password = "supersecret123";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password, "role", "CANDIDATE"))))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String accessTokenForEmployerWithCompany() throws Exception {
        String token = accessTokenForNewEmployer();
        mockMvc.perform(post("/api/v1/employers/me/company")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Atlas Automotive SARL", "sector", "automotive", "city", "Tangier"))))
                .andExpect(status().isOk());
        return token;
    }

    private Map<String, String> validJobBody() {
        return Map.of(
                "title", "Automotive QA Engineer",
                "description", "Inspect assembly line output for defects.",
                "sector", "automotive",
                "city", "Tangier",
                "contractType", "CDI");
    }

    @Test
    void createJobPosting_withCompany_createsDraft() throws Exception {
        String token = accessTokenForEmployerWithCompany();

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validJobBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Automotive QA Engineer"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void createJobPosting_withoutCompany_returns409() throws Exception {
        String token = accessTokenForNewEmployer();

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validJobBody())))
                .andExpect(status().isConflict());
    }

    @Test
    void createJobPosting_missingTitle_returns400() throws Exception {
        String token = accessTokenForEmployerWithCompany();

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "description", "desc", "sector", "automotive", "city", "Tangier", "contractType", "CDI"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createJobPosting_candidateToken_isForbidden() throws Exception {
        String token = accessTokenForNewCandidate();

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validJobBody())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createJobPosting_withoutAuth_isForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validJobBody())))
                .andExpect(status().isForbidden());
    }
}

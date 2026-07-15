package ma.jobstack.employer;

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
class EmployerCompanyTests {

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

    @Test
    void createCompany_thenGetReturnsIt() throws Exception {
        String token = accessTokenForNewEmployer();

        mockMvc.perform(post("/api/v1/employers/me/company")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Atlas Automotive SARL",
                                "sector", "automotive",
                                "city", "Tangier"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Atlas Automotive SARL"))
                .andExpect(jsonPath("$.verified").value(false));

        mockMvc.perform(get("/api/v1/employers/me/company").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Atlas Automotive SARL"))
                .andExpect(jsonPath("$.sector").value("automotive"))
                .andExpect(jsonPath("$.city").value("Tangier"));
    }

    @Test
    void createCompany_secondAttempt_returns409() throws Exception {
        String token = accessTokenForNewEmployer();
        Map<String, String> body = Map.of("name", "First Co", "sector", "automotive", "city", "Rabat");

        mockMvc.perform(post("/api/v1/employers/me/company")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/employers/me/company")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }

    @Test
    void getCompany_whenNoneExists_returns404() throws Exception {
        String token = accessTokenForNewEmployer();

        mockMvc.perform(get("/api/v1/employers/me/company").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCompany_missingName_returns400() throws Exception {
        String token = accessTokenForNewEmployer();

        mockMvc.perform(post("/api/v1/employers/me/company")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sector", "automotive"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void candidateToken_cannotCreateCompany() throws Exception {
        String token = accessTokenForNewCandidate();

        mockMvc.perform(post("/api/v1/employers/me/company")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Should Fail", "sector", "automotive", "city", "Rabat"))))
                .andExpect(status().isForbidden());
    }
}

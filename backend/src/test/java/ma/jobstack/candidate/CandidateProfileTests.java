package ma.jobstack.candidate;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.jobstack.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CandidateProfileTests {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String uniqueEmail() {
        return "candidate-" + UUID.randomUUID() + "@jobstack.ma";
    }

    private String accessTokenForNewCandidate() throws Exception {
        String email = uniqueEmail();
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
    void registeringAsCandidate_createsEmptyProfile_visibleViaGetMe() throws Exception {
        String token = accessTokenForNewCandidate();

        mockMvc.perform(get("/api/v1/candidates/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CANDIDATE"))
                .andExpect(jsonPath("$.fullName").doesNotExist())
                .andExpect(jsonPath("$.email").isNotEmpty())
                .andExpect(jsonPath("$.hasCv").value(false));
    }

    @Test
    void putMe_updatesProfileFields_andGetReflectsThem() throws Exception {
        String token = accessTokenForNewCandidate();

        mockMvc.perform(put("/api/v1/candidates/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Yasmine Alaoui",
                                "phone", "0612345678",
                                "sector", "IT",
                                "city", "Casablanca"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Yasmine Alaoui"))
                .andExpect(jsonPath("$.phone").value("0612345678"))
                .andExpect(jsonPath("$.sector").value("IT"))
                .andExpect(jsonPath("$.city").value("Casablanca"));

        mockMvc.perform(get("/api/v1/candidates/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Yasmine Alaoui"));
    }

    @Test
    void putMe_fullNameTooLong_returns400() throws Exception {
        String token = accessTokenForNewCandidate();

        mockMvc.perform(put("/api/v1/candidates/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("fullName", "x".repeat(201)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putMe_withoutAuth_isRejected() throws Exception {
        mockMvc.perform(put("/api/v1/candidates/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("fullName", "Someone"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void employerToken_cannotAccessCandidateProfile() throws Exception {
        String email = uniqueEmail();
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
        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();

        mockMvc.perform(get("/api/v1/candidates/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}

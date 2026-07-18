package ma.jobstack.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.jobstack.TestcontainersConfiguration;
import ma.jobstack.auth.User;
import ma.jobstack.auth.UserRepository;
import ma.jobstack.auth.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdminUserSuspensionTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String extractCookieValue(MvcResult result, String cookieName) {
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).isNotNull();
        Matcher matcher = Pattern.compile(cookieName + "=([^;]+)").matcher(setCookie);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String accessTokenForNewAdmin() throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@jobstack.ma";
        String password = "supersecret123";
        userRepository.save(new User(email, passwordEncoder.encode(password), UserRole.ADMIN));
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private record CandidateAccount(String userId, String email, String password, String refreshToken) {
    }

    private CandidateAccount registerAndLoginCandidate() throws Exception {
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
        String userId = userRepository.findByEmail(email).orElseThrow().getId().toString();
        String refreshToken = extractCookieValue(loginResult, "refresh_token");
        return new CandidateAccount(userId, email, password, refreshToken);
    }

    @Test
    void suspend_thenLogin_isRejected() throws Exception {
        CandidateAccount candidate = registerAndLoginCandidate();
        String adminToken = accessTokenForNewAdmin();

        mockMvc.perform(put("/api/v1/admin/users/" + candidate.userId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", candidate.email(), "password", candidate.password()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void suspend_thenRefresh_isRejected() throws Exception {
        CandidateAccount candidate = registerAndLoginCandidate();
        String adminToken = accessTokenForNewAdmin();

        mockMvc.perform(put("/api/v1/admin/users/" + candidate.userId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", candidate.refreshToken())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reactivate_afterSuspension_allowsLoginAgain() throws Exception {
        CandidateAccount candidate = registerAndLoginCandidate();
        String adminToken = accessTokenForNewAdmin();

        mockMvc.perform(put("/api/v1/admin/users/" + candidate.userId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/admin/users/" + candidate.userId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "ACTIVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", candidate.email(), "password", candidate.password()))))
                .andExpect(status().isOk());
    }

    @Test
    void setStatus_unknownUser_returns404() throws Exception {
        String adminToken = accessTokenForNewAdmin();
        mockMvc.perform(put("/api/v1/admin/users/" + UUID.randomUUID() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void setStatus_asNonAdmin_isForbidden() throws Exception {
        CandidateAccount candidate = registerAndLoginCandidate();
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", candidate.email(), "password", candidate.password()))))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", candidate.email(), "password", candidate.password()))))
                .andExpect(status().isOk())
                .andReturn();
        String candidateToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();

        mockMvc.perform(put("/api/v1/admin/users/" + candidate.userId() + "/status")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isForbidden());
    }
}

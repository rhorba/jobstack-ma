package ma.jobstack.candidate;

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

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "cv.storage.path=target/test-cvs")
class CvUploadTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final byte[] VALID_PDF_BYTES = "%PDF-1.4 fake but valid header".getBytes(StandardCharsets.UTF_8);

    private String uniqueEmail() {
        return "cv-" + UUID.randomUUID() + "@jobstack.ma";
    }

    private String accessTokenForNewCandidate() throws Exception {
        String email = uniqueEmail();
        String password = "supersecret123";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password, "role", "CANDIDATE"))))
                .andExpect(status().isOk());

        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    void uploadValidPdf_thenDownload_roundTrips() throws Exception {
        String token = accessTokenForNewCandidate();
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", VALID_PDF_BYTES);

        mockMvc.perform(multipart("/api/v1/candidates/me/cv").file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasCv").value(true));

        mockMvc.perform(get("/api/v1/candidates/me/cv").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().bytes(VALID_PDF_BYTES));
    }

    @Test
    void uploadNonPdfWithPdfExtension_rejectedByMagicByteCheck() throws Exception {
        String token = accessTokenForNewCandidate();
        MockMultipartFile fakeFile = new MockMultipartFile("file", "cv.pdf", "application/pdf",
                "this is actually just plain text, not a pdf".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/candidates/me/cv").file(fakeFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadOversizedFile_rejected() throws Exception {
        String token = accessTokenForNewCandidate();
        byte[] oversized = new byte[6 * 1024 * 1024];
        System.arraycopy(VALID_PDF_BYTES, 0, oversized, 0, VALID_PDF_BYTES.length);
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", oversized);

        mockMvc.perform(multipart("/api/v1/candidates/me/cv").file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void download_withoutUpload_returns404() throws Exception {
        String token = accessTokenForNewCandidate();

        mockMvc.perform(get("/api/v1/candidates/me/cv").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void upload_withoutAuth_isRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", VALID_PDF_BYTES);

        mockMvc.perform(multipart("/api/v1/candidates/me/cv").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    void twoCandidates_cannotSeeEachOthersCv() throws Exception {
        String tokenA = accessTokenForNewCandidate();
        String tokenB = accessTokenForNewCandidate();
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", VALID_PDF_BYTES);

        mockMvc.perform(multipart("/api/v1/candidates/me/cv").file(file)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Candidate B has no CV uploaded yet — must get their own 404, never A's file.
        mockMvc.perform(get("/api/v1/candidates/me/cv").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }
}

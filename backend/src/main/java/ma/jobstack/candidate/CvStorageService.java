package ma.jobstack.candidate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class CvStorageService {

    private static final byte[] PDF_MAGIC_BYTES = {'%', 'P', 'D', 'F', '-'};
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private final Path storageDirectory;

    public CvStorageService(@Value("${cv.storage.path}") String storagePath) {
        this.storageDirectory = Path.of(storagePath);
    }

    /** Filename is always server-derived from userId — never from client input — so path traversal is not possible. */
    public String store(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file provided");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File exceeds 5MB limit");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read uploaded file");
        }
        if (!hasPdfMagicBytes(content)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is not a valid PDF");
        }

        String filename = userId + ".pdf";
        try {
            Files.createDirectories(storageDirectory);
            Files.write(storageDirectory.resolve(filename), content);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to store CV file", e);
        }
        return filename;
    }

    public byte[] load(UUID userId) {
        Path path = storageDirectory.resolve(userId + ".pdf");
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No CV uploaded");
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read CV file", e);
        }
    }

    private boolean hasPdfMagicBytes(byte[] content) {
        if (content.length < PDF_MAGIC_BYTES.length) {
            return false;
        }
        for (int i = 0; i < PDF_MAGIC_BYTES.length; i++) {
            if (content[i] != PDF_MAGIC_BYTES[i]) {
                return false;
            }
        }
        return true;
    }
}

package gg.nya.imagehosting.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Service for managing data stored on the local disk.
 */
@Service
public class DataStorageService {

    @Value("${app.localstorage.thumbnail-directory}")
    private String thumbnailDirectory;
    @Value("${app.localstorage.temp-directory}")
    private String tempDirectory;

    private static final Logger log = LoggerFactory.getLogger(DataStorageService.class);

    /**
     * Stores a thumbnail image on the local disk at the specified path.
     * Creates user subdirectory if it doesn't exist.
     * 
     * @param username The username of the user
     * @param fileIdentifier The identifier of the file
     * @param inputStream The input stream containing the thumbnail data
     */
    public void saveThumbnail(String username, String fileIdentifier, InputStream inputStream) {
        log.trace("saveThumbnail, storing thumbnail for user {} with filename {}", username, fileIdentifier);

        try {
            Path userDir = Path.of(thumbnailDirectory, username);
            Files.createDirectories(userDir);
            
            Path thumbnailPath = userDir.resolve(fileIdentifier + ".png");
            Files.copy(inputStream, thumbnailPath, StandardCopyOption.REPLACE_EXISTING);
            
            log.debug("saveThumbnail, stored thumbnail for user {} with filename {} at path {}",
                    username, fileIdentifier, thumbnailPath);
        } catch (IOException e) {
            log.error("saveThumbnail, failed to store thumbnail for user {} with filename {}",
                    username, fileIdentifier, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store thumbnail", e);
        }
    }

    /**
     * Retrieves a thumbnail image from the local disk. If the thumbnail does not exist, retrieves a static backup thumbnail.
     *
     * @param username The username of the user
     * @param fileIdentifier The identifier (filename without extension)
     * @return An InputStream of the thumbnail image
     */
    public InputStream getThumbnail(String username, String fileIdentifier) {
        log.trace("getThumbnail, retrieving thumbnail for user {} with filename {}", username, fileIdentifier);

        Path thumbnailPath = Path.of(thumbnailDirectory, username, fileIdentifier + ".png");
        if (!Files.exists(thumbnailPath)) {
            log.warn("getThumbnail, thumbnail not found for user {} with filename {}, serving backup", username, fileIdentifier);
            return getBackupThumbnail();
        }

        try {
            log.trace("getThumbnail, found thumbnail for user {} with filename {}", username, fileIdentifier);
            return Files.newInputStream(thumbnailPath);
        } catch (IOException e) {
            log.error("getThumbnail, error while reading thumbnail from path {}", thumbnailPath);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading thumbnail", e);
        }
    }

    /**
     * Store a temporary file in the specified directory.
     * @param inputStream The input stream of the file to be stored.
     * @param filename The full name of the file, including its file extension.
     */
    public void saveTempFile(InputStream inputStream, String filename) {
        try {
            Path tempDir = Path.of(tempDirectory);
            Path tempInputFile = tempDir.resolve(filename);
            Files.createDirectories(tempDir);
            Files.createFile(tempInputFile);
            Files.copy(inputStream, tempInputFile, StandardCopyOption.REPLACE_EXISTING);
            log.debug("saveTempFile, created file {}", tempInputFile);
        } catch (IOException e) {
            log.error("saveTempFile, failed to store temporary file {}", filename, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store temporary file", e);
        }
    }

    /**
     * Returns the full path for a temporary file in the specified directory.
     * @param filename The full name of the file, including its file extension.
     * @return The path at which the file resides, if it exists with the given filename.
     */
    public Path getTempFilePath(String filename) {
        Path tempDir = Path.of(tempDirectory);
        return tempDir.resolve(filename);
    }

    /**
     * Attempts to delete a temporary file from the specified directory.
     * @param filename The name of the file to be removed (with extension).
     */
    public void deleteTempFile(String filename) {
        try {
            Path tempFile = Path.of(tempDirectory, filename);
            Files.deleteIfExists(tempFile);
            log.debug("deleteTempFile, deleted temporary file {}", tempFile);
        } catch (IOException e) {
            log.warn("deleteTempFile, failed to delete temporary file {}", filename, e);
        }
    }

    /**
     * Retrieves a backup thumbnail image from the compiled resources.
     *
     * @return An InputStream of the backup thumbnail image
     */
    private InputStream getBackupThumbnail() {
        return getClass().getResourceAsStream("/META-INF/resources/assets/images/backup.png");
    }
}

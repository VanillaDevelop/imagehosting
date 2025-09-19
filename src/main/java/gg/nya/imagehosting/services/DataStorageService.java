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

    private static final Logger log = LoggerFactory.getLogger(DataStorageService.class);

    //Directory where thumbnails are stored
    @Value("${thumbnails.storage.directory}")
    private String thumbnailDirectory;
    //Directory where temporary files are stored
    @Value("${media.temp.directory}")
    private String tempDirectory;

    /**
     * Stores a thumbnail image on the local disk at the specified path.
     * Creates user directory if it doesn't exist.
     * 
     * @param username The username of the user
     * @param filename The identifier (filename without extension)
     * @param inputStream The input stream containing the thumbnail data
     */
    public void storeThumbnail(String username, String filename, InputStream inputStream) {
        try {
            Path userDir = Path.of(thumbnailDirectory, username);
            Files.createDirectories(userDir);
            
            Path thumbnailPath = userDir.resolve(filename + ".png");
            Files.copy(inputStream, thumbnailPath, StandardCopyOption.REPLACE_EXISTING);
            
            log.debug("storeThumbnail, stored thumbnail for user {} with filename {} at path {}", 
                    username, filename, thumbnailPath);
        } catch (IOException e) {
            log.error("storeThumbnail, failed to store thumbnail for user {} with filename {}", 
                    username, filename, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store thumbnail", e);
        }
    }

    /**
     * Retrieves a thumbnail image from the local disk. If the thumbnail does not exist, retrieves a static backup thumbnail.
     *
     * @param username The username of the user
     * @param filename The identifier (filename without extension)
     * @return An InputStream of the thumbnail image
     */
    public InputStream retrieveThumbnail(String username, String filename) {
        Path thumbnailPath = Path.of(thumbnailDirectory, username, filename + ".png");
        if (!Files.exists(thumbnailPath)) {
            log.trace("retrieveThumbnail, thumbnail not found for user {} with filename {}, serving backup", username, filename);
            return retrieveBackupThumbnail();
        }
        log.debug("retrieveThumbnail, found thumbnail for user {} with filename {}", username, filename);

        try {
            return Files.newInputStream(thumbnailPath);
        } catch (IOException e) {
            log.error("retrieveThumbnail, error while reading thumbnail from path {}", thumbnailPath);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading thumbnail", e);
        }
    }

    /**
     * Store a temporary file in the specified directory.
     * @param inputStream The input stream of the file to be stored.
     * @param filename The name of the file (with extension).
     * @return The path to the stored temporary file
     * @throws RuntimeException if an error occurs while storing the file.
     */
    public Path storeTempFile(InputStream inputStream, String filename) {
        try {
            Path tempDir = Path.of(tempDirectory);
            Path tempInputFile = tempDir.resolve(filename);
            Files.createDirectories(tempDir);
            Files.createFile(tempInputFile);
            Files.copy(inputStream, tempInputFile, StandardCopyOption.REPLACE_EXISTING);
            log.debug("storeTempFile, created file {}", tempInputFile);
            return tempInputFile;
        } catch (IOException e) {
            log.error("storeTempFile, failed to store temporary file {}", filename, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error storing temporary file", e);
        }
    }

    /**
     * Retrieves a backup thumbnail image from the compiled resources.
     *
     * @return An InputStream of the backup thumbnail image
     */
    private InputStream retrieveBackupThumbnail() {
        InputStream backupStream = getClass().getResourceAsStream("/META-INF/resources/assets/images/backup.png");
        if (backupStream == null) {
            log.error("retrieveBackupThumbnail, backup thumbnail not found in resources");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Missing server resource");
        }
        log.trace("retrieveBackupThumbnail, retrieved backup thumbnail from resources");
        return backupStream;
    }
}

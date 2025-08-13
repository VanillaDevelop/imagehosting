package gg.nya.imagehosting.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class StaticDataService {

    private static final Logger log = LoggerFactory.getLogger(StaticDataService.class);

    @Value("${thumbnails.storage.directory}")
    private String thumbnailDirectory;

    /**
     * Stores a thumbnail image on the local disk at the specified path.
     * Creates user directory if it doesn't exist.
     * 
     * @param username The username of the user
     * @param filename The filename (without extension)
     * @param inputStream The input stream containing the thumbnail data
     * @throws RuntimeException if an error occurs while storing the thumbnail
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
            throw new RuntimeException("Failed to store thumbnail", e);
        }
    }

    /**
     * Retrieves a thumbnail image from the local disk.
     *
     * @param username The username of the user
     * @param filename The filename (without extension)
     * @return An InputStream of the thumbnail image
     * @throws RuntimeException if the thumbnail does not exist or an error occurs while retrieving it
     */
    public InputStream retrieveThumbnail(String username, String filename) {
        try {
            Path thumbnailPath = Path.of(thumbnailDirectory, username, filename + ".png");
            if (!Files.exists(thumbnailPath)) {
                log.warn("retrieveThumbnail, thumbnail not found for user {} with filename {}", username, filename);
                throw new RuntimeException("Thumbnail not found");
            }
            log.debug("retrieveThumbnail, retrieved thumbnail for user {} with filename {} from path {}",
                    username, filename, thumbnailPath);
            return Files.newInputStream(thumbnailPath);
        } catch (IOException e) {
            log.error("retrieveThumbnail, failed to retrieve thumbnail for user {} with filename {}",
                    username, filename, e);
            throw new RuntimeException("Failed to retrieve thumbnail", e);
        }
    }

    /**
     * Retrieves a backup thumbnail image from the resources.
     *
     * @return An InputStream of the backup thumbnail image
     * @throws RuntimeException if the backup thumbnail is not found
     */
    public InputStream retrieveBackupThumbnail() {
        InputStream backupStream = getClass().getResourceAsStream("/META-INF/resources/assets/images/backup.png");
        if (backupStream == null) {
            log.error("retrieveBackupThumbnail, backup thumbnail not found in resources");
            throw new RuntimeException("Backup thumbnail not found");
        }
        log.debug("retrieveBackupThumbnail, retrieved backup thumbnail from resources");
        return backupStream;
    }
}

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
}

package gg.nya.imagehosting.utils;

import gg.nya.imagehosting.models.ImageHostingModes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class Utils {
    private Utils() {
    }

    /**
     * Extract the username from the server name.
     *
     * @param serverName The server name.
     * @return The username, based on the subdomain.
     */
    public static String extractUsernameFromServerName(String serverName) {
        String subdomain = serverName.substring(0, serverName.indexOf("."));

        if (subdomain.isBlank() || !subdomain.matches("^[a-zA-Z0-9_-]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not extract username from request");
        }

        return subdomain;
    }

    /**
     * Extract the media type from an image file name.
     *
     * @param filename The file name.
     * @return The media type.
     */
    public static MediaType getMediaTypeFromFilename(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename");
        }

        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.valueOf("image/webp");
            case "svg" -> MediaType.valueOf("image/svg+xml");
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown file type");
        };
    }

    /**
     * Generates a filename based on the provided strategy.
     *
     * @param strategy The strategy to use.
     * @return A filename based on the provided strategy.
     */
    public static String generateFilenameFromStrategy(ImageHostingModes strategy) {
        return switch (strategy) {
            case UUID -> UUID.randomUUID().toString();
            case ALPHANUMERIC -> generateRandomAlphanumericString(8);
            case TIMESTAMPED -> generateTimestampedFilename();
        };
    }

    /**
     * Generates a random alphanumeric string of the provided length.
     *
     * @param length The length of the string.
     * @return A random alphanumeric string of the provided length.
     */
    private static String generateRandomAlphanumericString(int length) {
        String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * characters.length());
            sb.append(characters.charAt(index));
        }
        return sb.toString();
    }

    /**
     * Generates a timestamped filename.
     */
    private static String generateTimestampedFilename() {
        return LocalDateTime.now().toString().replaceAll(":. ", "-");
    }
}

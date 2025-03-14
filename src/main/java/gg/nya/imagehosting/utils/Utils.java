package gg.nya.imagehosting.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;

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
        try {
            String subdomain = serverName.substring(0, serverName.indexOf("."));

            if (subdomain.isBlank() || !subdomain.matches("^[a-zA-Z0-9_-]+$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid subdomain");
            }

            return subdomain;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not extract username from request");
        }
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
        try {
            String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            return switch (ext) {
                case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
                case "png" -> MediaType.IMAGE_PNG;
                case "gif" -> MediaType.IMAGE_GIF;
                case "webp" -> MediaType.valueOf("image/webp");
                case "svg" -> MediaType.valueOf("image/svg+xml");
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown file type");

            };
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename format");
        }
    }
}

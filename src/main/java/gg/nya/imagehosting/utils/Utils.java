package gg.nya.imagehosting.utils;

import gg.nya.imagehosting.models.HostingMode;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Static utility class, mainly for module-independent String operations.
 */
public abstract class Utils {
    static final String ALPHANUMERIC_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    static final int ALPHANUMERIC_LENGTH = 8;

    private Utils() {
    }

    /**
     * Extracts the leading subdomain (up to the first dot) from a URI.
     * Returns an empty string if no subdomain is found.
     *
     * @param uri The URI to extract the subdomain from.
     * @return The extracted subdomain.
     */
    public static String getLeadingSubdomainFromUri(String uri) {
        if(uri == null || !uri.contains(".")) return "";
        return uri.substring(0, uri.indexOf("."));
    }

    /**
     * Extracts the trailing resource (from the last slash to the end or first query parameter) from a URI.
     * Returns an empty string if no slash is found.
     *
     * @param uri The URI to extract the resource from.
     * @return The extracted resource.
     */
    public static String getTrailingResourceFromUri(String uri) {
        if(uri == null) return "";
        if(!uri.contains("/")) return "";
        if(uri.lastIndexOf("/") + 1 >= uri.length()) return "";
        String uriAfterLastSlash = uri.substring(uri.lastIndexOf("/") + 1);
        if(uriAfterLastSlash.contains("?")) {
            return uriAfterLastSlash.substring(0, uriAfterLastSlash.indexOf("?"));
        } else {
            return uriAfterLastSlash;
        }
    }

    /**
     * Extracts the file extension from a filename.
     * Returns an empty string if no extension is found.
     *
     * @param filename The filename to extract the extension from.
     * @return The extracted file extension.
     */
    public static String getFileExtensionFromFilename(String filename) {
        if(filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Generates a filename based on the provided strategy.
     *
     * @param strategy The strategy to use.
     * @return A filename based on the provided strategy.
     */
    public static String generateFilenameFromStrategy(HostingMode strategy) {
        return switch (strategy) {
            case UUID -> UUID.randomUUID().toString();
            case ALPHANUMERIC -> generateRandomAlphanumericString();
            case TIMESTAMPED -> generateTimestampedFilename();
        };
    }

    /**
     * Creates a resource URL for an uploaded resource.
     *
     * @param request  The original request, to identify the active server URL.
     * @param username The username of the user.
     * @param category The category that identifies the resource type.
     * @param filename The filename of the resource.
     * @return The URL to retrieve the resource.
     */
    public static String createResourceURL(HttpServletRequest request, String username, String category, String filename) {
        return createResourceURL(
                request.getScheme(),
                request.getServerName(),
                request.getServerPort(),
                username,
                category,
                filename
        );
    }

    /**
     * Creates a resource URL for an uploaded resource.
     *
     * @param scheme The scheme of the active server (http or https).
     * @param serverName The server name of the active server.
     * @param serverPort The server port of the active server.
     * @param username The username of the user (can be null to create an URL at the main domain).
     * @param category The category that identifies the resource type.
     * @param filename The filename of the resource.
     * @return The URL to retrieve the resource.
     */
    public static String createResourceURL(String scheme, String serverName, int serverPort, String username,
                                           String category, String filename) {
        String port = serverPort == 80 || serverPort == 443 ? "" : ":" + serverPort;
        String usernameAddition = username == null ? "" : username + ".";
        String baseUrl = scheme + "://" + usernameAddition + serverName + port;
        return baseUrl + "/" + category + "/" + filename;
    }

    /**
     * Generates a random alphanumeric string of the provided length.
     *
     * @return A random alphanumeric string of the provided length.
     */
    private static String generateRandomAlphanumericString() {
        StringBuilder sb = new StringBuilder(ALPHANUMERIC_LENGTH);
        for (int i = 0; i < ALPHANUMERIC_LENGTH; i++) {
            int index = (int) (Math.random() * ALPHANUMERIC_CHARACTERS.length());
            sb.append(ALPHANUMERIC_CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    /**
     * Generates a timestamped filename.
     */
    private static String generateTimestampedFilename() {
        return LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy-HHmmss"));
    }
}

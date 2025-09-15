package gg.nya.imagehosting.utils;


import gg.nya.imagehosting.models.ImageApiEntity;
import jakarta.servlet.http.HttpServletRequest;

public abstract class RESTUtils {
    private RESTUtils() {
    }

    /**
     * Creates the REST Entity that should be returned for a successful image upload.
     *
     * @param request  The request when POSTing the image
     * @param username The username of the user
     * @param filename The filename of the image
     * @return The REST Entity
     */
    public static ImageApiEntity createImageApiEntityResponse(HttpServletRequest request, String username, String filename) {
        ImageApiEntity imageApiEntity = new ImageApiEntity();
        imageApiEntity.setURL(RESTUtils.fetchURLFromRequest(request, username, "i", filename, true));
        return imageApiEntity;
    }

    /**
     * Builds the URL to retrieve the image.
     *
     * @param request  The request when POSTing the image
     * @param username The username of the user
     * @param filename The filename of the image
     * @return The URL to retrieve the image
     */
    public static String fetchURLFromRequest(HttpServletRequest request, String username, String category, String filename, boolean pureVideo) {
        return fetchURLFromLocation(
                request.getScheme(),
                request.getServerName(),
                request.getServerPort(),
                username,
                category,
                filename,
                pureVideo
        );
    }

    public static String fetchURLFromLocation(String scheme, String serverName, int serverPort, String username,
                                              String category, String filename, boolean pureVideo) {
        String port = serverPort == 80 ? "" : ":" + serverPort;
        String baseUrl = scheme + "://" + username + "." + serverName + port;
        String fileUrl = baseUrl + "/" + category + "/" + filename;
        if (pureVideo) {
            return fileUrl;
        }
        return fileUrl.substring(0, fileUrl.lastIndexOf('.'));
    }
}

package gg.nya.imagehosting.beans;

import gg.nya.imagehosting.models.VideoUploadStatus;
import gg.nya.imagehosting.models.VideoUploadUserFile;
import gg.nya.imagehosting.services.AuthenticationService;
import gg.nya.imagehosting.services.VideoHostingService;
import gg.nya.imagehosting.utils.Utils;
import jakarta.faces.context.FacesContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

import java.io.Serial;
import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Request-scoped bean for managing video playback.
 */
@Component
@Scope("request")
public class VideoPlayerBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private VideoUploadUserFile video;
    private String username;
    private String thumbnailUrl;
    private String playerUrl;
    private String videoUrl;

    private final AuthenticationService authenticationService;

    private static final Logger log = LoggerFactory.getLogger(VideoPlayerBean.class);

    /**
     * Constructor for VideoPlayerBean.
     * Derives video information from the request URI and fetches video details from the VideoHostingService.
     * The bean will automatically redirect to the home page if the requested video is not found.
     *
     * @param videoHostingService The video hosting service to fetch video details from.
     * @param request The HTTP servlet request, which defines the video through its filename and the user subdomain.
     * @param authenticationService The authentication service to check if the user is logged in.
     */
    @Autowired
    public VideoPlayerBean(VideoHostingService videoHostingService, HttpServletRequest request,
                           AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;

        // Extract data for display
        String serverName = request.getServerName();
        String username = Utils.getLeadingSubdomainFromUri(serverName);
        //Contains the original URI before forwarding through WebMvcConfig
        String originalUri = (String) request.getAttribute("jakarta.servlet.forward.request_uri");
        String filename = Utils.getTrailingResourceFromUri(originalUri != null ? originalUri : request.getRequestURI());

        Optional<VideoUploadUserFile> videoOpt = videoHostingService.getVideoMetadata(username, filename);
        if(videoOpt.isEmpty()) {
            log.warn("Unknown video {} for user {} requested, redirecting", filename, username);
            FacesContext.getCurrentInstance().getApplication().getNavigationHandler().handleNavigation(
                    FacesContext.getCurrentInstance(),
                    null,
                    "/index?faces-redirect=true"
            );
            return;
        }

        this.video = videoOpt.get();

        // We do this query because the display name that the user used to register might have different capitalization from the subdomain
        this.username = video.getVideoUploadUser().getUser().getDisplayName();
        // Construct URLs - we do not need to append the username as the request is already made to a subdomain
        this.thumbnailUrl = Utils.createResourceURL(
                request,
                null,
                "thumbnails",
                "v-" + video.getFileName() + ".png"
        );
        this.playerUrl = Utils.createResourceURL(
                request,
                null,
                "v",
                video.getFileName()
        );
        this.videoUrl = Utils.createResourceURL(
                request,
                null,
                "v",
                video.getFileName() + ".mp4"
        );
    }

    /**
     * Checks if a user is currently logged in.
     * @return True if a user is logged in, false otherwise.
     */
    public boolean isUserLoggedIn() {
        return authenticationService.isCurrentUserAuthenticated();
    }

    /**
     * Gets the title of the video.
     * @return The video title
     */
    public String getVideoTitle() {
        if(video == null) return null;
        return video.getVideoTitle();
    }

    /**
     * Gets the URL to retrieve the raw video file (player source)
     * @return The video URL
     */
    public String getVideoUrl() {
        return videoUrl;
    }

    /**
     * Gets the URL of the video's thumbnail image.
     * @return The thumbnail URL
     */
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    /**
     * Gets the URL to retrieve the URL to the video player page.
     * @return The player URL
     */
    public String getPlayerUrl() {
        return playerUrl;
    }

    /**
     * Gets the username of the video's owner.
     * @return The owner's username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the current upload status of the video.
     * @return The video's upload status
     */
    public VideoUploadStatus getVideoStatus() {
        if(video == null) return VideoUploadStatus.FAILED;
        return video.getUploadStatus();
    }

    /**
     * Gets the timestamp of when the video was created, formatted as "yyyy-MM-dd HH:mm:ss".
     * @return The formatted creation timestamp
     */
    public String getTimestamp() {
        if(video == null) return "";
        return video.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}

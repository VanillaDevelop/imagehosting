package gg.nya.imagehosting.beans;

import gg.nya.imagehosting.models.VideoUploadStatus;
import gg.nya.imagehosting.models.VideoUploadUserFile;
import gg.nya.imagehosting.services.AuthenticationService;
import gg.nya.imagehosting.services.VideoHostingService;
import gg.nya.imagehosting.utils.RESTUtils;
import gg.nya.imagehosting.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Request-scoped bean for managing video playback.
 */
@Component
@Scope("request")
public class VideoPlayerBean {
    private final VideoUploadUserFile video;
    private final String username;
    private final String thumbnailUrl;
    private final String playerUrl;
    private final String videoUrl;
    private final AuthenticationService authenticationService;

    @Autowired
    public VideoPlayerBean(VideoHostingService videoHostingService, HttpServletRequest request, AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
        String serverName = request.getServerName();
        String username = Utils.extractUsernameFromServerName(serverName);
        //Contains the original URI before forwarding through WebMvcConfig
        String originalUri = (String) request.getAttribute("jakarta.servlet.forward.request_uri");
        String filename = extractFilenameFromUri(originalUri != null ? originalUri : request.getRequestURI());
        Optional<VideoUploadUserFile> videoOpt = videoHostingService.getVideoData(username, filename);
        this.video = videoOpt.orElse(null);
        if(this.video != null) {
            // We do this query because the display name that the user used to register might have different capitalization
            this.username = video.getVideoUploadUser().getUser().getDisplayName();
            this.thumbnailUrl = RESTUtils.fetchURLWithoutUsername(
                    request.getScheme(),
                    request.getServerName(),
                    request.getServerPort(),
                    "thumbnails",
                    "v-" + video.getFileName() + ".png"
            );
            this.playerUrl = RESTUtils.fetchURLWithoutUsername(
                    request.getScheme(),
                    request.getServerName(),
                    request.getServerPort(),
                    "v",
                    video.getFileName()
            );
            this.videoUrl = RESTUtils.fetchURLWithoutUsername(
                    request.getScheme(),
                    request.getServerName(),
                    request.getServerPort(),
                    "v",
                    video.getFileName() + ".mp4"
            );
        } else {
            this.username = "Unknown";
            this.thumbnailUrl = "#";
            this.playerUrl = "#";
            this.videoUrl = "#";
        }
    }

    public boolean isUserLoggedIn() {
        return authenticationService.isCurrentUserAuthenticated();
    }

    public String getVideoTitle() {
        return video.getVideoTitle();
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getPlayerUrl() {
        return playerUrl;
    }

    public String getUsername() {
        return username;
    }

    public VideoUploadStatus getVideoStatus() {
        return video.getUploadStatus();
    }

    public String getTimestamp() {
        return video.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String extractFilenameFromUri(String uri) {
        int lastSlashIndex = uri.lastIndexOf('/');
        return lastSlashIndex >= 0 ? uri.substring(lastSlashIndex + 1) : uri;
    }
}

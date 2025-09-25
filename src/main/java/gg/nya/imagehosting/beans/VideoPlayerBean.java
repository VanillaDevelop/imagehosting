package gg.nya.imagehosting.beans;

import gg.nya.imagehosting.models.VideoUploadUserFile;
import gg.nya.imagehosting.services.VideoHostingService;
import gg.nya.imagehosting.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Request-scoped bean for managing video playback.
 */
@Component
@Scope("request")
public class VideoPlayerBean {
    private final VideoUploadUserFile video;

    @Autowired
    public VideoPlayerBean(VideoHostingService videoHostingService, HttpServletRequest request) {
        String serverName = request.getServerName();
        String username = Utils.extractUsernameFromServerName(serverName);
        //Contains the original URI before forwarding through WebMvcConfig
        String originalUri = (String) request.getAttribute("jakarta.servlet.forward.request_uri");
        String filename = extractFilenameFromUri(originalUri != null ? originalUri : request.getRequestURI());
        Optional<VideoUploadUserFile> videoOpt = videoHostingService.getVideoData(username, filename);
        this.video = videoOpt.orElse(null);
    }

    public String getVideoTitle() {
        return video.getVideoTitle();
    }

    public String getVideoUrl() {
        return "/v/" + video.getFileName() + ".mp4";
    }

    private String extractFilenameFromUri(String uri) {
        int lastSlashIndex = uri.lastIndexOf('/');
        return lastSlashIndex >= 0 ? uri.substring(lastSlashIndex + 1) : uri;
    }
}

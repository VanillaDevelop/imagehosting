package gg.nya.imagehosting.beans;

import gg.nya.imagehosting.config.ApplicationContextProvider;
import gg.nya.imagehosting.models.VideoUploadUserFile;
import gg.nya.imagehosting.services.AuthenticationService;
import gg.nya.imagehosting.services.VideoHostingService;
import gg.nya.imagehosting.utils.RESTUtils;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * View-scoped bean for managing and displaying a user's video library.
 */
@Component
@Scope("view")
public class VideoLibraryBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private transient VideoHostingService videoHostingService;
    private transient AuthenticationService authenticationService;

    private static final Logger log = LoggerFactory.getLogger(VideoLibraryBean.class);

    private final List<VideoUploadUserFile> videos = new ArrayList<>();
    private boolean canLoadMoreVideos = true;
    private int page = 0;
    private final String requestScheme;
    private final String serverName;
    private final int serverPort;

    /**
     * Constructor for VideoLibraryBean.
     * The servlet is injected at the original request to capture the scheme, server name, and port.
     * This information is used to construct full URLs for video access.
     *
     * @param videoHostingService The video hosting service to fetch user videos.
     * @param authenticationService The authentication service to get current user information.
     * @param httpServletRequest The HTTP servlet request to extract scheme, server name, and port.
     */
    @Autowired
    public VideoLibraryBean(VideoHostingService videoHostingService, AuthenticationService authenticationService, HttpServletRequest httpServletRequest) {
        this.videoHostingService = videoHostingService;
        this.authenticationService = authenticationService;
        this.requestScheme = httpServletRequest.getScheme();
        this.serverName = httpServletRequest.getServerName();
        this.serverPort = httpServletRequest.getServerPort();
    }

    /**
     * Initializes the bean by loading the first page of videos for the current user.
     */
    @PostConstruct
    public void init() {
        log.info("init, loading initial videos for user: {}", authenticationService.getCurrentUsername());
        this.loadMoreVideos();
    }

    /**
     * Loads more videos for the current user if available.
     * Increments the page number after loading.
     * If the number of retrieved videos is less than the maximum, sets canLoadMoreVideos to false.
     */
    public void loadMoreVideos() {
        if(!canLoadMoreVideos) {
            log.warn("loadMoreVideos, cannot load more videos, already reached the end of the list");
            return;
        }

        log.info("loadMoreVideos, user {} loading more videos, loading page: {}",
                authenticationService.getCurrentUsername(), page);

        List<VideoUploadUserFile> newVideos = videoHostingService.getVideos(page, 10,
                authenticationService.getCurrentUsername());
        if (newVideos.isEmpty() || newVideos.size() < 10) {
            log.debug("loadMoreVideos, no more videos to load for user {}, page: {}, set canLoadMoreVideos=false",
                     authenticationService.getCurrentUserId(), page);
            canLoadMoreVideos = false;
        }

        log.debug("loadMoreVideos, loaded {} videos for user {}, page: {}",
                 newVideos.size(), authenticationService.getCurrentUserId(), page);
        videos.addAll(newVideos);
        page++;
    }

    /**
     * Getter for video list - returns a copy.
     * @return List of videos loaded for the user.
     */
    public List<VideoUploadUserFile> getVideos() {
        return List.copyOf(videos);
    }

    /**
     * Returns the thumbnail URL for a given video. This is a relative URL.
     * @param video The video for which to get the thumbnail URL.
     * @return The thumbnail URL as a String.
     */
    public String getThumbnailUrl(VideoUploadUserFile video) {
        return "/thumbnails/v-" + video.getFileName();
    }

    /**
     * Returns the full URL for accessing a given video.
     * @param video The video for which to get the full URL.
     * @return The full video URL as a String.
     */
    public String getVideoUrl(VideoUploadUserFile video) {
        return RESTUtils.fetchURLFromLocation(requestScheme, serverName, serverPort,
                authenticationService.getCurrentUsername(), "v", video.getFileName());
    }

    /**
     * Returns the title of the video, or the filename if the title is not set.
     * @param video The video for which to get the title.
     * @return The video title or filename as a String.
     */
    public String getVideoTitle(VideoUploadUserFile video) {
        String title = video.getVideoTitle();
        if(title == null || title.isBlank()) {
            return video.getFileName();
        }
        return title;
    }

    /**
     * Indicates whether more videos can be loaded.
     * @return true if more videos can be loaded, false otherwise.
     */
    public boolean isCanLoadMoreVideos() {
        return canLoadMoreVideos;
    }

    /**
     * Returns the creation date of the video as a String.
     * @param video The video for which to get the creation date.
     * @return The creation date as a String.
     */
    public String getCreatedAt(VideoUploadUserFile video) {
        return video.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Returns the appropriate text for the load more videos button based on whether more videos can be loaded.
     * @return The button text as a String.
     */
    public String getLoadVideosButtonText() {
        if (canLoadMoreVideos) {
            return "Load More Videos";
        } else {
            return "End of Video List";
        }
    }

    /**
     * Rehydrates transient services after deserialization.
     * @param in The ObjectInputStream from which the object is being deserialized.
     */
    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // Retrieve the Spring application context (using a helper or static context holder)
        ApplicationContext ctx = ApplicationContextProvider.getApplicationContext();
        this.videoHostingService = ctx.getBean(VideoHostingService.class);
        this.authenticationService = ctx.getBean(AuthenticationService.class);
    }
}

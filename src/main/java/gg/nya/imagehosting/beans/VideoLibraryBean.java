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
import java.util.ArrayList;
import java.util.List;

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

    @Autowired
    public VideoLibraryBean(VideoHostingService videoHostingService, AuthenticationService authenticationService, HttpServletRequest httpServletRequest) {
        this.videoHostingService = videoHostingService;
        this.authenticationService = authenticationService;
        this.requestScheme = httpServletRequest.getScheme();
        this.serverName = httpServletRequest.getServerName();
        this.serverPort = httpServletRequest.getServerPort();
    }

    @PostConstruct
    public void init() {
        log.info("VideoLibraryBean initialized for user: {}", authenticationService.getCurrentUsername());
        this.loadMoreVideos();
    }

    public void loadMoreVideos() {
        if(!canLoadMoreVideos) {
            log.warn("loadMoreVideos, cannot load more videos, already reached the end of the list");
            return;
        }

        log.info("loadMoreVideos, user {} loading more videos, loading page: {}",
                authenticationService.getCurrentUserId(), page);

        List<VideoUploadUserFile> newVideos = videoHostingService.getVideos(page, 10, authenticationService.getCurrentUsername());
        if (newVideos.isEmpty() || newVideos.size() < 10) {
            log.info("loadMoreVideos, no more videos to load for user {}, page: {}",
                     authenticationService.getCurrentUserId(), page);
            canLoadMoreVideos = false;
        }

        log.info("loadMoreVideos, loaded {} videos for user {}, page: {}",
                 newVideos.size(), authenticationService.getCurrentUserId(), page);
        videos.addAll(newVideos);
        page++;
    }

    public List<VideoUploadUserFile> getVideos() {
        return videos;
    }

    public String getThumbnailUrl(VideoUploadUserFile video) {
        return "/thumbnails/" + video.getFileName();
    }

    public String getVideoUrl(VideoUploadUserFile video) {
        return RESTUtils.fetchURLFromLocation(requestScheme, serverName, serverPort,
                authenticationService.getCurrentUsername(), "v", video.getFileName());
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // Retrieve the Spring application context (using a helper or static context holder)
        ApplicationContext ctx = ApplicationContextProvider.getApplicationContext();
        this.videoHostingService = ctx.getBean(VideoHostingService.class);
        this.authenticationService = ctx.getBean(AuthenticationService.class);
    }
}

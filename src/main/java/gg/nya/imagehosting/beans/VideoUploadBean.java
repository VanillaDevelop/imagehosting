package gg.nya.imagehosting.beans;

import gg.nya.imagehosting.config.ApplicationContextProvider;
import gg.nya.imagehosting.models.HostingMode;
import gg.nya.imagehosting.models.VideoUploadUser;
import gg.nya.imagehosting.services.AuthenticationService;
import gg.nya.imagehosting.services.VideoHostingService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;

@Component
@Scope("view")
public class VideoUploadBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private VideoUploadUser videoUploadUser;

    private transient VideoHostingService videoHostingService;
    private transient AuthenticationService authenticationService;

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(VideoUploadBean.class);

    @Autowired
    public VideoUploadBean(VideoHostingService videoHostingService, AuthenticationService authenticationService) {
        this.videoHostingService = videoHostingService;
        this.authenticationService = authenticationService;
    }

    @PostConstruct
    public void init() {
        log.debug("init, fetching video upload user for user ID {}", authenticationService.getCurrentUserId());
        this.videoUploadUser = videoHostingService.getOrCreateVideoUploadUser(authenticationService.getCurrentUserId());
    }

    public HostingMode getCurrentUploadMode() {
        return videoUploadUser.getVideoUploadMode();
    }

    @Serial
    private void readObject(ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        ApplicationContext ctx = ApplicationContextProvider.getApplicationContext();
        this.videoHostingService = ctx.getBean(VideoHostingService.class);
        this.authenticationService = ctx.getBean(AuthenticationService.class);
    }
}

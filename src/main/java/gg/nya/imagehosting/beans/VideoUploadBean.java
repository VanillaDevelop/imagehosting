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
import java.util.List;

/**
 * View-scoped bean for managing video uploads.
 */
@Component
@Scope("view")
public class VideoUploadBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private VideoUploadUser videoUploadUser;

    private transient VideoHostingService videoHostingService;
    private transient AuthenticationService authenticationService;

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(VideoUploadBean.class);

    /**
     * Constructor for VideoUploadBean.
     * Injects services.
     *
     * @param videoHostingService The video hosting service to manage video uploads.
     * @param authenticationService The authentication service to get current user information.
     */
    @Autowired
    public VideoUploadBean(VideoHostingService videoHostingService, AuthenticationService authenticationService) {
        this.videoHostingService = videoHostingService;
        this.authenticationService = authenticationService;
    }

    /**
     * Initializes the bean by fetching or creating the VideoUploadUser for the current user.
     */
    @PostConstruct
    public void init() {
        log.info("init, fetching video upload user for user ID {}", authenticationService.getCurrentUserId());
        this.videoUploadUser = videoHostingService.getOrCreateVideoUploadUser(authenticationService.getCurrentUserId());
    }

    /**
     * Gets the VideoUploadUser associated with the current user.
     * @return The VideoUploadUser object.
     */
    public VideoUploadUser getVideoUploadUser() {
        return this.videoUploadUser;
    }

    /**
     * Updates the VideoUploadUser settings in the database.
     */
    public void updateUser() {
        videoHostingService.saveVideoUploadUser(this.videoUploadUser);
    }

    /**
     * Retrieves the list of available video upload modes.
     * @return A list of all HostingMode values.
     */
    public List<HostingMode> getAvailableUploadModes() {
        return List.of(HostingMode.values());
    }

    /**
     * Rehydrates transient services after deserialization.
     * @param in The ObjectInputStream from which the object is being deserialized.
     */
    @Serial
    private void readObject(ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        ApplicationContext ctx = ApplicationContextProvider.getApplicationContext();
        this.videoHostingService = ctx.getBean(VideoHostingService.class);
        this.authenticationService = ctx.getBean(AuthenticationService.class);
    }
}

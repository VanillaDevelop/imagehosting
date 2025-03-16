package gg.nya.imagehosting.beans;

import gg.nya.imagehosting.config.ApplicationContextProvider;
import gg.nya.imagehosting.models.ImageHostingUser;
import gg.nya.imagehosting.security.UserSession;
import gg.nya.imagehosting.services.ImageHostingService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.io.Serializable;

@Component("imageHostingBean")
@Scope("view")
public class ImageHostingBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private ImageHostingUser imageHostingUser;

    private final UserSession userSession;
    private transient ImageHostingService imageHostingService;

    private static final Logger log = LoggerFactory.getLogger(ImageHostingBean.class);

    @Autowired
    public ImageHostingBean(ImageHostingService imageHostingService, UserSession userSession) {
        this.imageHostingService = imageHostingService;
        this.userSession = userSession;
    }

    @PostConstruct
    public void init() {
        log.debug("init, fetching image hosting user for user ID {}", userSession.getUserId());
        this.imageHostingUser = imageHostingService.getOrCreateImageHostingUser(userSession.getUserId());
    }

    public String getApiKey() {
        return imageHostingUser.getApiKey();
    }

    @Serial
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        ApplicationContext ctx = ApplicationContextProvider.getApplicationContext();
        this.imageHostingService = ctx.getBean(ImageHostingService.class);
    }
}

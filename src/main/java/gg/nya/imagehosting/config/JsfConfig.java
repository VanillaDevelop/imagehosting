package gg.nya.imagehosting.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import jakarta.servlet.ServletContext;

@Configuration
public class JsfConfig implements ServletContextInitializer {

    @Override
    public void onStartup(ServletContext sc) {
        // Configure JSF specific settings
        sc.setInitParameter("javax.faces.FACELETS_SKIP_COMMENTS", "true");
        sc.setInitParameter("javax.faces.PROJECT_STAGE", "Development");
        sc.setInitParameter("com.sun.faces.forceLoadConfiguration", "true");

        // CSRF handling
        sc.setInitParameter("jakarta.faces.ALWAYS_PERFORM_VALIDATION_WHEN_REQUIRED", "true");
    }
}

package gg.nya.imagehosting.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(@NonNull ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.xhtml");
        registry.addViewController("/v/{filename:^(?!.*\\.mp4$).*$}").setViewName("forward:/videodisplay.xhtml");
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }
}

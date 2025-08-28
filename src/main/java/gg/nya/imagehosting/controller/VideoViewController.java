package gg.nya.imagehosting.controller;

import gg.nya.imagehosting.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class VideoViewController {

    private static final Logger log = LoggerFactory.getLogger(VideoViewController.class);

    @GetMapping(value = "/v/{filename:^(?!.*\\.mp4$).*$}")
    public String getVideoView(@PathVariable String filename, HttpServletRequest request) {
        String serverName = request.getServerName();
        String user = Utils.extractUsernameFromServerName(serverName);
        log.info("getVideoView, video view requested for user {}, filename: {}", user, filename);
        
        // Set attributes for the view
        request.setAttribute("filename", filename);
        request.setAttribute("user", user);
        request.setAttribute("videoUrl", "/v/" + filename + ".mp4");
        return "/video-view.xhtml";
    }
}
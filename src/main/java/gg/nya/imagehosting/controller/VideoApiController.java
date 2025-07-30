package gg.nya.imagehosting.controller;

import gg.nya.imagehosting.services.AuthenticationService;
import gg.nya.imagehosting.services.VideoHostingService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@RestController
public class VideoApiController {
    private final VideoHostingService videoHostingService;
    private final AuthenticationService authenticationService;

    private static final Logger log = LoggerFactory.getLogger(VideoApiController.class);

    @Autowired
    public VideoApiController(VideoHostingService videoHostingService, AuthenticationService authenticationService) {
        this.videoHostingService = videoHostingService;
        this.authenticationService = authenticationService;
    }

    @PostMapping(value = "/v/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadVideo(
            HttpServletRequest request,
            @RequestParam("videoInput") MultipartFile videoFile,
            @RequestParam("startTimeSeconds") double startTimeSeconds,
            @RequestParam("endTimeSeconds") double endTimeSeconds,
            @RequestParam("videoTitle") String videoTitle,
            @RequestParam("fullVideo") boolean fullVideo
            ) throws IOException {

        long userId = this.authenticationService.getCurrentUserId();
        log.info("uploadVideo, video upload requested for user with ID {}, original file name {}, start time: {}, end time: {}, full video: {}, requested title: {}",
                userId, videoFile.getOriginalFilename(), startTimeSeconds, endTimeSeconds, fullVideo, videoTitle);

        InputStream videoInputStream = videoFile.getInputStream();
        String generatedUrl = videoHostingService.uploadVideoForUser(
                request,
                userId,
                videoInputStream,
                videoFile.getOriginalFilename(),
                startTimeSeconds,
                endTimeSeconds,
                videoTitle,
                fullVideo
        );

        return "redirect:" + generatedUrl;

    }

}

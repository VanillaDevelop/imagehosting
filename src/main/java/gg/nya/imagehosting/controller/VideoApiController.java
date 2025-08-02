package gg.nya.imagehosting.controller;

import gg.nya.imagehosting.services.AuthenticationService;
import gg.nya.imagehosting.services.VideoHostingService;
import gg.nya.imagehosting.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

import java.io.ByteArrayInputStream;
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

    @GetMapping(value = "/v/{filename}", produces = MediaType.ALL_VALUE)
    public ResponseEntity<InputStreamResource> getVideo(@PathVariable String filename, HttpServletRequest request) {
        String serverName = request.getServerName();
        String user = Utils.extractUsernameFromServerName(serverName);
        log.info("getVideo, video requested for user {}, filename: {}", user, filename);
        ByteArrayInputStream videoStream = videoHostingService.retrieveVideo(user, filename);
        videoStream.reset();
        MediaType contentType = MediaType.parseMediaType("video/webm");

        return ResponseEntity.ok()
                .contentType(contentType)
                .body(new InputStreamResource(videoStream));
    }

    @PostMapping(value = {"/v", "/v/"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RedirectView uploadVideo(
            HttpServletRequest request,
            @RequestParam("videoInput") MultipartFile videoFile,
            @RequestParam("startTimeSeconds") double startTimeSeconds,
            @RequestParam("endTimeSeconds") double endTimeSeconds,
            @RequestParam("videoTitle") String videoTitle
            ) throws IOException {

        long userId = this.authenticationService.getCurrentUserId();
        log.info("uploadVideo, video upload requested for user with ID {}, original file name {}, start time: {}, end time: {}, requested title: {}",
                userId, videoFile.getOriginalFilename(), startTimeSeconds, endTimeSeconds, videoTitle);

        InputStream videoInputStream = videoFile.getInputStream();
        String generatedUrl = videoHostingService.uploadVideoForUser(
                request,
                userId,
                videoInputStream,
                videoFile.getOriginalFilename(),
                startTimeSeconds,
                endTimeSeconds,
                videoTitle
        );

        return new RedirectView(generatedUrl);
    }

}

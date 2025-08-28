package gg.nya.imagehosting.controller;

import gg.nya.imagehosting.services.AuthenticationService;
import gg.nya.imagehosting.services.VideoHostingService;
import gg.nya.imagehosting.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

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

    @GetMapping(value = "/v/{filename:.*\\.mp4$}", produces = MediaType.ALL_VALUE)
    public ResponseEntity<ByteArrayResource> getVideo(@PathVariable String filename, HttpServletRequest request) {
        
        String serverName = request.getServerName();
        String user = Utils.extractUsernameFromServerName(serverName);
        log.info("getVideo, video requested for user {}, filename: {}", user, filename);
        
        ByteArrayInputStream videoStream = videoHostingService.retrieveVideo(user, filename);
        videoStream.reset(); // Reset position to beginning
        byte[] videoData;
        videoData = videoStream.readAllBytes();

        long contentLength = videoData.length;
        String rangeHeader = request.getHeader("Range");
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept-Ranges", "bytes");
        headers.add("Cache-Control", "public, max-age=31536000");
        headers.setContentType(MediaType.parseMediaType("video/mp4"));
        
        // Handle Range requests
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.substring(6).split("-");
            long start = Long.parseLong(ranges[0]);
            long end = ranges.length > 1 && !ranges[1].isEmpty() ? 
                      Long.parseLong(ranges[1]) : contentLength - 1;
            
            end = Math.min(end, contentLength - 1);
            long rangeLength = end - start + 1;
            
            headers.add("Content-Range", String.format("bytes %d-%d/%d", start, end, contentLength));
            headers.setContentLength(rangeLength);
            
            byte[] rangeData = Arrays.copyOfRange(videoData, (int)start, (int)end + 1);
            
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .body(new ByteArrayResource(rangeData));
        }
        
        // Full content response
        headers.setContentLength(contentLength);
        return ResponseEntity.ok()
                .headers(headers)
                .body(new ByteArrayResource(videoData));
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

package gg.nya.imagehosting.controller;

import gg.nya.imagehosting.services.AuthenticationService;
import gg.nya.imagehosting.services.VideoHostingService;
import gg.nya.imagehosting.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.io.InputStream;

/**
 * This REST controller handles video upload and retrieval requests at the /v/ endpoint.
 */
@RestController
public class VideoApiController {
    private final VideoHostingService videoHostingService;
    private final AuthenticationService authenticationService;

    private static final Logger log = LoggerFactory.getLogger(VideoApiController.class);

    /**
     * Constructor for VideoApiController.
     * Injects the relevant services.
     *
     * @param videoHostingService Service for handling video upload and retrieval.
     * @param authenticationService Service for retrieving the currently authenticated user.
     */
    @Autowired
    public VideoApiController(VideoHostingService videoHostingService, AuthenticationService authenticationService) {
        this.videoHostingService = videoHostingService;
        this.authenticationService = authenticationService;
    }

    /**
     * Retrieve a video file. Supports HTTP Range requests for partial content delivery.
     * This endpoint only handles requests that end with .mp4 and serves raw video data.
     * For requests without .mp4, the player embed is served via WebMvcConfig.
     *
     * @param filename The name of the video file to retrieve - must end with .mp4
     * @param request The HTTP request, for extracting username and request headers.
     * @return ResponseEntity containing the video data and appropriate headers.
     */
    @GetMapping(value = "/v/{filename:.*}.mp4", produces = "video/mp4")
    public ResponseEntity<InputStreamResource> getVideo(@PathVariable String filename, HttpServletRequest request) {
        // Identify file to serve
        String serverName = request.getServerName();
        String user = Utils.getLeadingSubdomainFromUri(serverName);
        log.info("getVideo, video file requested for user {}, filename: {}", user, filename);

        // Check if file exists via content length check
        // Will throw 404 if the video does not exist on the database or S3
        final long contentLength = videoHostingService.getVideoFileSize(user, filename);

        //Check if request contains Range header
        String rangeHeader = request.getHeader("Range");
        final long start, end;

        //Simple case - no Range header, serve full content
        if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
            start = 0;
            end = contentLength - 1;
        }
        //Parse Range header
        else {
            String[] ranges = rangeHeader.substring(6).split("-");
            start = ranges[0].isEmpty() ? 0 : Long.parseLong(ranges[0]);
            if (start >= contentLength) {
                log.error("getVideo, invalid range request: start {} >= contentLength {} for user {}, filename {}",
                        start, contentLength, user, filename);
                throw new ResponseStatusException(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, "Invalid Range Header");
            }
            end = ranges.length > 1 && !ranges[1].isEmpty() ? Long.parseLong(ranges[1]) : contentLength - 1;
            if(end < start) {
                log.error("getVideo, invalid range request: end {} < start {} for user {}, filename {}",
                        end, start, user, filename);
                throw new ResponseStatusException(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, "Invalid Range Header");
            }
            if(end >= contentLength) {
                log.error("getVideo, invalid range request: end {} >= contentLength {} for user {}, filename {}",
                        end, contentLength, user, filename);
                throw new ResponseStatusException(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, "Invalid Range Header");
            }
        }
        final long rangeLength = end - start + 1;

        // Set response headers
        HttpHeaders headers = new HttpHeaders();
        // Tell the client that range requests are supported
        headers.add("Accept-Ranges", "bytes");
        // Tell the client to cache the video for 1 year - the video URL is permanent and immutable
        headers.add("Cache-Control", "public, max-age=31536000");
        // Content-Range header is set only for partial content responses
        if (rangeHeader != null) {
            headers.add("Content-Range", String.format("bytes %d-%d/%d", start, end, contentLength));
        }
        // Content-Length is always set to the length of the response body
        headers.setContentLength(rangeLength);
        HttpStatus status = rangeHeader != null ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK;
        InputStream in = videoHostingService.getVideo(user, filename, start, end);

        // Synchronous IO streaming because otherwise Weld complains - should be fine for low to medium traffic levels
        return ResponseEntity.status(status).headers(headers).body(new InputStreamResource(in));
    }

    /**
     * Upload a video file as a multipart/form-data POST request.
     *
     * @param request The HTTP request, for building the correct video URL in the service.
     * @param videoFile The uploaded video file.
     * @param startTimeSeconds The start time in seconds for trimming the video.
     * @param endTimeSeconds The end time in seconds for trimming the video.
     * @param videoTitle The title of the video.
     * @return A RedirectView to the URL of the uploaded video.
     */
    @PostMapping(value = {"/v", "/v/"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RedirectView uploadVideo(
            HttpServletRequest request,
            @RequestParam("videoInput") MultipartFile videoFile,
            @RequestParam("startTimeSeconds") double startTimeSeconds,
            @RequestParam("endTimeSeconds") double endTimeSeconds,
            @RequestParam("videoTitle") String videoTitle
            ) {

        String username = authenticationService.getCurrentUsername();
        log.info("uploadVideo, video upload requested for user {}, original file name {}, start time: {}, end time: {}, requested title: {}",
                username, videoFile.getOriginalFilename(), startTimeSeconds, endTimeSeconds, videoTitle);

        try {
            //IOException may be thrown here if the file is not accessible
            InputStream videoInputStream = videoFile.getInputStream();

            // Delegate to service to handle the upload
            String generatedIdentifier = videoHostingService.saveVideo(
                    username,
                    videoInputStream,
                    videoFile.getOriginalFilename(),
                    startTimeSeconds,
                    endTimeSeconds,
                    videoTitle
            );
            return new RedirectView(Utils.createResourceURL(request, username, "v", generatedIdentifier));
        } catch (IOException e) {
            //This error occurs early enough that no cleanup is necessary
            log.error("uploadVideo, error obtaining input stream from uploaded file for user {}, original file name {}",
                    username, videoFile.getOriginalFilename(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing uploaded file");
        }


    }

}

package gg.nya.imagehosting.controller;

import gg.nya.imagehosting.services.FFmpegService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@RestController
public class VideoApiController {
    private final FFmpegService ffmpegService;

    private static final Logger log = LoggerFactory.getLogger(VideoApiController.class);

    @Autowired
    public VideoApiController(FFmpegService ffmpegService) {
        this.ffmpegService = ffmpegService;
    }

    @PostMapping(value = "/v/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadVideo(
            @RequestParam("videoInput") MultipartFile videoFile,
            @RequestParam("startTimeSeconds") double startTimeSeconds,
            @RequestParam("endTimeSeconds") double endTimeSeconds) throws IOException {
        
        InputStream videoInputStream = videoFile.getInputStream();
        InputStream convertedVideo = this.ffmpegService.convertToMp4(videoInputStream, videoFile.getOriginalFilename(),
                startTimeSeconds, endTimeSeconds);

        return ResponseEntity.ok().body("todo");
    }

}

package gg.nya.imagehosting.controller;

import gg.nya.imagehosting.models.ImageApiEntity;
import gg.nya.imagehosting.services.ImageHostingService;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;

@RestController
public class ImageApiController {
    private final ImageHostingService imageHostingService;

    private static final Logger log = LoggerFactory.getLogger(ImageApiController.class);

    @Autowired
    public ImageApiController(ImageHostingService imageHostingService) {
        this.imageHostingService = imageHostingService;
    }

    @GetMapping(value = "/i/{filename}", produces = MediaType.ALL_VALUE)
    public ResponseEntity<InputStreamResource> getImage(@PathVariable String filename, HttpServletRequest request) {
        String serverName = request.getServerName();
        String user = Utils.extractUsernameFromServerName(serverName);
        log.info("getImage, image requested for user {}, filename: {}", user, filename);
        ByteArrayInputStream imageStream = imageHostingService.retrieveImage(user, filename);
        imageStream.reset();
        MediaType contentType = Utils.getImageTypeFromFileName(filename);

        return ResponseEntity.ok()
                .contentType(contentType)
                .body(new InputStreamResource(imageStream));
    }

    @PostMapping(value = "/i/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageApiEntity> uploadImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) throws IOException {
        String apiKey = request.getHeader("X-API-Key");
        log.info("uploadImage, image upload requested for user with API key {}, original file name {}", apiKey, file.getOriginalFilename());
        ImageApiEntity response = imageHostingService.uploadImageForUser(request, apiKey, file.getInputStream(), file.getOriginalFilename());
        return ResponseEntity.ok().body(response);
    }
}

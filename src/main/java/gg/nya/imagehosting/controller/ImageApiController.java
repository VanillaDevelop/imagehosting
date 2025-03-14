package gg.nya.imagehosting.controller;

import gg.nya.imagehosting.services.S3Service;
import gg.nya.imagehosting.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;

@RestController
public class ImageApiController {
    private final S3Service s3Service;

    @Autowired
    public ImageApiController(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @GetMapping(value = "/i/{filename}", produces = MediaType.ALL_VALUE)
    public ResponseEntity<InputStreamResource> getImage(@PathVariable String filename, HttpServletRequest request) {
        String serverName = request.getServerName();
        String user = Utils.extractUsernameFromServerName(serverName);
        InputStream imageStream = s3Service.getImage(user, filename);
        MediaType contentType = Utils.getMediaTypeFromFilename(filename);

        return ResponseEntity.ok()
                .contentType(contentType)
                .body(new InputStreamResource(imageStream));
    }
}

package gg.nya.imagehosting.controller;

import gg.nya.imagehosting.services.AuthenticationService;
import gg.nya.imagehosting.services.DataStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * This REST controller handles requests for user-specific thumbnails at the /thumbnails/ endpoint.
 */
@RestController
public class ThumbnailApiController {
    private final DataStorageService dataStorageService;
    private final AuthenticationService authenticationService;

    private static final Logger log = LoggerFactory.getLogger(ThumbnailApiController.class);

    /**
     * Constructor for ThumbnailApiController.
     * Injects the relevant services.
     *
     * @param dataStorageService Service for handling disk data (thumbnail) retrieval.
     * @param authenticationService Service for retrieving the currently authenticated user.
     */
    @Autowired
    public ThumbnailApiController(DataStorageService dataStorageService, AuthenticationService authenticationService) {
        this.dataStorageService = dataStorageService;
        this.authenticationService = authenticationService;
    }

    /**
     * Retrieves a thumbnail for the current user.
     * If the thumbnail does not exist, it tries to retrieve a static backup thumbnail.
     *
     * @param filename The identifier of the thumbnail (filename without extension).
     * @return ResponseEntity containing the thumbnail image as an InputStreamResource
     */
    @GetMapping(value ="/thumbnails/{filename}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<InputStreamResource> getThumbnailForCurrentUser(@PathVariable String filename) {
        String user = authenticationService.getCurrentUsername();
        log.info("getThumbnailForCurrentUser, thumbnail requested for user {}, filename: {}", user, filename);

        InputStreamResource thumbnail = new InputStreamResource(dataStorageService.retrieveThumbnail(user, filename));
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(thumbnail);
    }
}

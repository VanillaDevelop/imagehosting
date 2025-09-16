package gg.nya.imagehosting.controller;

import gg.nya.imagehosting.services.AuthenticationService;
import gg.nya.imagehosting.services.StaticDataService;
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
    private final StaticDataService staticDataService;
    private final AuthenticationService authenticationService;

    private static final Logger log = LoggerFactory.getLogger(ThumbnailApiController.class);

    /**
     * Constructor for ThumbnailApiController.
     * Injects the relevant services.
     *
     * @param staticDataService Service for handling static data (thumbnail) retrieval.
     * @param authenticationService Service for retrieving the currently authenticated user.
     */
    @Autowired
    public ThumbnailApiController(StaticDataService staticDataService, AuthenticationService authenticationService) {
        this.staticDataService = staticDataService;
        this.authenticationService = authenticationService;
    }

    /**
     * Retrieves a thumbnail for the current user.
     * If the thumbnail does not exist, it tries to retrieve a static backup thumbnail.
     *
     * @param filename The filename of the thumbnail (without extension)
     * @return ResponseEntity with the thumbnail image or 404 if not found
     */
    @GetMapping(value ="/thumbnails/{filename}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<InputStreamResource> getThumbnailForCurrentUser(@PathVariable String filename) {
        String user = authenticationService.getCurrentUsername();
        log.info("getThumbnailForCurrentUser, thumbnail requested for user {}, filename: {}", user, filename);

        try {
            InputStreamResource thumbnail = new InputStreamResource(staticDataService.retrieveThumbnail(user, filename));
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(thumbnail);
        } catch (RuntimeException e) {
            //Try retrieving the backup thumbnail instead
            log.warn("getThumbnailForCurrentUser, failed to retrieve thumbnail for user {}, filename: {}, error: {}",
                    user, filename, e.getMessage());
            try {
                InputStreamResource backupThumbnail = new InputStreamResource(staticDataService.retrieveBackupThumbnail());
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .body(backupThumbnail);
            } catch (RuntimeException backupException) {
                log.error("getThumbnailForCurrentUser, failed to retrieve backup thumbnail, error: {}, returning 404",
                        backupException.getMessage());
                if (backupException.getCause() != null) {
                    log.error("getThumbnailForCurrentUser, cause: {}", backupException.getCause().getMessage());
                }
                return ResponseEntity.notFound().build();
            }
        }
    }
}

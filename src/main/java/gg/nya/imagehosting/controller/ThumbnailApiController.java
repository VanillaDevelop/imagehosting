package gg.nya.imagehosting.controller;

import gg.nya.imagehosting.services.DataStorageService;
import gg.nya.imagehosting.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
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

    private static final Logger log = LoggerFactory.getLogger(ThumbnailApiController.class);

    /**
     * Constructor for ThumbnailApiController.
     * Injects the relevant services.
     *
     * @param dataStorageService Service for handling disk data (thumbnail) retrieval.
     */
    @Autowired
    public ThumbnailApiController(DataStorageService dataStorageService) {
        this.dataStorageService = dataStorageService;
    }

    /**
     * Retrieves a thumbnail for the given user and filename.
     * If the thumbnail does not exist, it tries to retrieve a static backup thumbnail.
     * The caller may request the thumbnail with or without the .png extension - this is mainly for compatibility with
     * metadata extractors that expect file extensions.
     *
     * @param filename The identifier of the thumbnail (filename with or without .png extension).
     * @param request The HTTP request, for extracting the username.
     * @return ResponseEntity containing the thumbnail image as an InputStreamResource
     */
    @GetMapping(value = {"/thumbnails/{filename}", "/thumbnails/{filename}.png"}, produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<InputStreamResource> getThumbnail(@PathVariable String filename, HttpServletRequest request) {
        // Identify file to serve
        String serverName = request.getServerName();
        String user = Utils.getLeadingSubdomainFromUri(serverName);
        log.info("getThumbnail, thumbnail requested for user {}, filename: {}", user, filename);

        InputStreamResource thumbnail = new InputStreamResource(dataStorageService.retrieveThumbnail(user, filename));
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(thumbnail);
    }
}

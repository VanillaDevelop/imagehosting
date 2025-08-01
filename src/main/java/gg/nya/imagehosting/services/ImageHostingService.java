package gg.nya.imagehosting.services;

import gg.nya.imagehosting.models.ImageApiEntity;
import gg.nya.imagehosting.models.ImageHostingUser;
import gg.nya.imagehosting.models.ImageHostingUserFile;
import gg.nya.imagehosting.models.User;
import gg.nya.imagehosting.repositories.ImageHostingUserFileRepository;
import gg.nya.imagehosting.repositories.ImageHostingUserRepository;
import gg.nya.imagehosting.utils.RESTUtils;
import gg.nya.imagehosting.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ImageHostingService {
    private final ImageHostingUserRepository imageHostingUserRepository;
    private final ImageHostingUserFileRepository imageHostingUserFileRepository;
    private final UserService userService;
    private final S3Service s3Service;

    private static final Logger log = LoggerFactory.getLogger(ImageHostingService.class);

    @Autowired
    public ImageHostingService(ImageHostingUserRepository imageHostingUserRepository,
                               ImageHostingUserFileRepository imageHostingUserFileRepository,
                               UserService userService, S3Service s3Service) {
        this.imageHostingUserRepository = imageHostingUserRepository;
        this.imageHostingUserFileRepository = imageHostingUserFileRepository;
        this.userService = userService;
        this.s3Service = s3Service;
    }

    /**
     * Serve an image from the given user.
     *
     * @param username The username of the user.
     * @param filename The filename of the image.
     * @return The image as an input stream, if it exists. Throws a 404 error if the image does not exist.
     */
    public ByteArrayInputStream retrieveImage(String username, String filename) {
        log.debug("retrieveImage, checking if image for user {} with filename {} exists", username, filename);
        if (!checkImageExists(username, filename)) {
            log.error("retrieveImage, image for user {} with filename {} not found", username, filename);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
        }
        log.debug("retrieveImage, image for user {} with filename {} found in DB, querying cached S3", username, filename);
        return s3Service.getFile(username, filename);
    }

    /**
     * Finds or creates an image hosting user for the given user.
     *
     * @param userId The user ID to find or create an image hosting user for.
     * @return The image hosting user for the given user.
     */
    public ImageHostingUser getOrCreateImageHostingUser(Long userId) {
        log.debug("getOrCreateImageHostingUser, getting or creating image hosting user for user ID {}", userId);
        //Refresh user which might be outdated
        Optional<User> updatedUserOpt = userService.getUserById(userId);
        if (updatedUserOpt.isEmpty()) {
            log.error("getOrCreateImageHostingUser, user ID {} not found", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find user when refreshing");
        }
        User updatedUser = updatedUserOpt.get();

        Optional<ImageHostingUser> imageHostingUserOpt = imageHostingUserRepository.findImageHostingUserByUser(updatedUser);
        if (imageHostingUserOpt.isPresent()) {
            log.debug("getOrCreateImageHostingUser, image hosting user for user {} found", updatedUser.getUsername());
            return imageHostingUserOpt.get();
        }

        //Create and persist new image hosting user
        log.debug("getOrCreateImageHostingUser, creating new image hosting user for user {}", updatedUser.getUsername());
        ImageHostingUser imageHostingUser = new ImageHostingUser(updatedUser);
        imageHostingUserRepository.save(imageHostingUser);
        return imageHostingUser;
    }


    /**
     * Uploads an image for the user associated with the given API key.
     *
     * @param request          The request when POSTing the image
     * @param apiKey           The API key of the user.
     * @param fileStream       The image to upload.
     * @param originalFileName The original file name.
     * @return The REST entity to retrieve this image.
     * @throws IOException If the image could not be uploaded.
     */
    public ImageApiEntity uploadImageForUser(HttpServletRequest request, String apiKey, InputStream fileStream, String originalFileName) throws IOException {
        log.debug("uploadImageForUser, attempting to upload image for user with API key {}", apiKey);
        ImageHostingUser user = validateApiKey(apiKey);
        //Just a check to see if we can parse the file type
        Utils.getImageTypeFromFileName(originalFileName);
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        log.debug("uploadImageForUser, uploading image for user {} with file type {}", user.getUser().getUsername(), fileExtension);
        //Generate a file name for the image
        String fileName = tryCreateFileName(user, fileExtension);
        //Upload image to S3
        s3Service.uploadImage(user.getUser().getUsername(), fileName, fileStream);
        //Create and persist new image hosting user file
        ImageHostingUserFile imageHostingUserFile = new ImageHostingUserFile();
        imageHostingUserFile.setImageHostingUser(user);
        imageHostingUserFile.setFileName(fileName);
        imageHostingUserFile.setFileSize((long) fileStream.available());
        imageHostingUserFile.setCreatedAt(LocalDateTime.now());
        imageHostingUserFileRepository.save(imageHostingUserFile);
        //Generate REST response
        ImageApiEntity response = RESTUtils.createImageApiEntityResponse(request, user.getUser().getUsername(), fileName);
        log.debug("uploadImageForUser, uploaded image for user {} with filename {}", user.getUser().getUsername(), fileName);
        return response;
    }

    private boolean checkImageExists(String username, String filename) {
        Optional<ImageHostingUser> imageHostingUserOpt = imageHostingUserRepository.findImageHostingUserByUsername(username);
        if (imageHostingUserOpt.isEmpty()) {
            log.error("checkImageExists, image hosting user for user {} not found", username);
            return false;
        }
        //Check if this user has a file with the given filename
        if (!imageHostingUserFileRepository.existsByImageHostingUserAndFileName(imageHostingUserOpt.get(), filename)) {
            log.error("checkImageExists, image hosting user for user {} does not have a file with filename {}", username, filename);
            return false;
        }
        return true;
    }


    /**
     * Attempts to create a file name for the user's given strategy. Aborts with a 500 error after 100 failed attempts.
     *
     * @return The file name.
     */
    private String tryCreateFileName(ImageHostingUser imageHostingUser, String fileExtension) {
        for (int i = 0; i < 100; i++) {
            String fileName = Utils.generateFilenameFromStrategy(imageHostingUser.getImageHostingMode()) + fileExtension.toLowerCase();
            if (!imageHostingUserFileRepository.existsByImageHostingUserAndFileName(imageHostingUser, fileName)) {
                log.debug("tryCreateFileName, determined file name {} for image hosting user {}", fileName, imageHostingUser.getId());
                return fileName;
            }
        }
        log.error("tryCreateFileName, could not create file name for user {} with strategy {}",
                imageHostingUser.getUser().getUsername(), imageHostingUser.getImageHostingMode());
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create file for user.");
    }

    /**
     * Finds the user associated with the given API key. Throws a 404 error if the API key is invalid.
     *
     * @param apiKey The API key to check
     * @return The image hosting user associated with the given API key.
     */
    private ImageHostingUser validateApiKey(String apiKey) {
        Optional<ImageHostingUser> imageHostingUserOpt = imageHostingUserRepository.findImageHostingUserByApiKey(apiKey);
        if (imageHostingUserOpt.isEmpty()) {
            log.error("validateApiKey, image hosting user with API key {} not found", apiKey);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find image hosting user");
        }
        log.debug("validateApiKey, resolving API key {} to image hosting user {}", apiKey, imageHostingUserOpt.get().getId());
        return imageHostingUserOpt.get();
    }
}

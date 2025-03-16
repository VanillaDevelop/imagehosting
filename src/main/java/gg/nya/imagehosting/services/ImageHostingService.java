package gg.nya.imagehosting.services;

import gg.nya.imagehosting.models.ImageHostingUser;
import gg.nya.imagehosting.models.User;
import gg.nya.imagehosting.repositories.ImageHostingUserFileRepository;
import gg.nya.imagehosting.repositories.ImageHostingUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class ImageHostingService {
    private final ImageHostingUserRepository imageHostingUserRepository;
    private final ImageHostingUserFileRepository imageHostingUserFileRepository;
    private final UserService userService;

    private static final Logger log = LoggerFactory.getLogger(ImageHostingService.class);

    @Autowired
    public ImageHostingService(ImageHostingUserRepository imageHostingUserRepository,
                               ImageHostingUserFileRepository imageHostingUserFileRepository,
                               UserService userService) {
        this.imageHostingUserRepository = imageHostingUserRepository;
        this.imageHostingUserFileRepository = imageHostingUserFileRepository;
        this.userService = userService;
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
}

package gg.nya.imagehosting.services;

import gg.nya.imagehosting.models.User;
import gg.nya.imagehosting.models.VideoUploadStatus;
import gg.nya.imagehosting.models.VideoUploadUser;
import gg.nya.imagehosting.models.VideoUploadUserFile;
import gg.nya.imagehosting.repositories.UserRepository;
import gg.nya.imagehosting.repositories.VideoUploadUserFileRepository;
import gg.nya.imagehosting.repositories.VideoUploadUserRepository;
import gg.nya.imagehosting.utils.RESTUtils;
import gg.nya.imagehosting.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Service
public class VideoHostingService {

    private static final Logger log = LoggerFactory.getLogger(VideoHostingService.class);

    private final VideoUploadUserRepository videoUploadUserRepository;
    private final VideoUploadUserFileRepository videoUploadUserFileRepository;
    private final UserRepository userRepository;

    @Autowired
    public VideoHostingService(VideoUploadUserRepository videoUploadUserRepository,
                               VideoUploadUserFileRepository userFileRepository, UserRepository userRepository) {
       this.videoUploadUserRepository = videoUploadUserRepository;
         this.videoUploadUserFileRepository = userFileRepository;
       this.userRepository = userRepository;
    }

    public String uploadVideoForUser(
            HttpServletRequest request,
            long userId,
            InputStream videoInputStream,
            String originalFileName,
            double startTimeSeconds,
            double endTimeSeconds,
            String videoTitle,
            boolean fullVideo) throws IOException {
        log.debug("uploadVideoForUser, attempting to upload video for user with ID {}", userId);

        Optional<User> userOpt = userRepository.findById(userId);
        if(userOpt.isEmpty()) {
            log.error("uploadVideoForUser, user with ID {} not found", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find user");
        }

        Optional<VideoUploadUser> videoUser = videoUploadUserRepository.findVideoUploadUserByUser(userOpt.get().getUsername());
        if(videoUser.isEmpty()) {
            log.error("uploadVideoForUser, video upload user for username {} not found", userOpt.get().getUsername());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find video upload user");
        }

        //Check if we can parse the file type
        MediaType fileType = Utils.getVideoTypeFromFileName(originalFileName);
        log.debug("uploadVideoForUser, uploading video for user {} with file type {}", userOpt.get().getUsername(),
                fileType);

        //Create new file name
        String newFileName = tryCreateFileName(videoUser.get());

        //Process video upload asynchronously
        //TODO

        // Save the video file to the repository
        log.debug("uploadVideoForUser, saving video file for user {} with file name {}", userOpt.get().getUsername(), newFileName);
        VideoUploadUserFile videoUploadUserFile = new VideoUploadUserFile();
        videoUploadUserFile.setVideoUploadUser(videoUser.get());
        videoUploadUserFile.setFileName(newFileName);
        videoUploadUserFile.setFileSize((long) videoInputStream.available());
        videoUploadUserFile.setCreatedAt(java.time.LocalDateTime.now());
        videoUploadUserFile.setUploadStatus(VideoUploadStatus.PROCESSING);
        videoUploadUserFileRepository.save(videoUploadUserFile);

        // Return the file name for further processing
        return RESTUtils.fetchURLFromRequest(request, userOpt.get().getUsername(), "v", newFileName);
    }


    /**
     * Attempts to create a file name for the user's given strategy. Aborts with a 500 error after 100 failed attempts.
     *
     * @return The file name.
     */
    private String tryCreateFileName(VideoUploadUser videoUploadUser) {
        for (int i = 0; i < 100; i++) {
            String fileName = Utils.generateFilenameFromStrategy(videoUploadUser.getVideoUploadMode()) + ".webm";
            if (!videoUploadUserFileRepository.existsByVideoUploadUserAndFileName(videoUploadUser, fileName)) {
                log.debug("tryCreateFileName, determined file name {} for image hosting user {}", fileName, videoUploadUser.getId());
                return fileName;
            }
        }
        log.error("tryCreateFileName, could not create file name for user {} with strategy {}",
                videoUploadUser.getUser().getUsername(), videoUploadUser.getVideoUploadMode());
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create file for user.");
    }
}

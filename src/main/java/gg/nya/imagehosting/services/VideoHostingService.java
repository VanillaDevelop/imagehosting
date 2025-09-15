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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Service
public class VideoHostingService {

    private static final Logger log = LoggerFactory.getLogger(VideoHostingService.class);

    private final VideoUploadUserRepository videoUploadUserRepository;
    private final VideoUploadUserFileRepository videoUploadUserFileRepository;
    private final UserRepository userRepository;
    private final MediaManagementService mediaManagementService;
    private final S3Service s3Service;

    @Autowired
    public VideoHostingService(VideoUploadUserRepository videoUploadUserRepository,
                               VideoUploadUserFileRepository userFileRepository, UserRepository userRepository, MediaManagementService mediaManagementService, S3Service s3Service) {
       this.videoUploadUserRepository = videoUploadUserRepository;
         this.videoUploadUserFileRepository = userFileRepository;
       this.userRepository = userRepository;
        this.mediaManagementService = mediaManagementService;
        this.s3Service = s3Service;
    }

    /**
     * Serve a video from the given user.
     *
     * @param username The username of the user.
     * @param filename The filename of the video.
     * @return The video as an input stream, if it exists. Throws a 404 error if the video does not exist.
     */
    public ByteArrayInputStream retrieveVideo(String username, String filename) {
        log.debug("retrieveVideo, checking if video for user {} with filename {} exists", username, filename);
        if (!checkVideoExists(username, filename)) {
            log.error("retrieveVideo, video for user {} with filename {} not found", username, filename);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found");
        }
        log.debug("retrieveVideo, video for user {} with filename {} found in DB, querying cached S3", username, filename);
        return s3Service.getFile(username, filename);
    }

    /**
     * Uploads a video for the user associated with the given User ID.
     * @param request   The HTTP request, used to construct the URL for the uploaded video.
     * @param userId The ID of the user for whom the video is being uploaded.
     * @param videoInputStream The InputStream containing the video data to be uploaded.
     * @param originalFileName The original file name of the video being uploaded.
     * @param startTimeSeconds The start time in seconds for the video segment, if applicable.
     * @param endTimeSeconds The end time in seconds for the video segment, if applicable.
     * @param videoTitle The title of the video being uploaded.
     * @return The URL of the uploaded video file.
     * @throws IOException If an error occurs while processing the video upload.
     */
    public String uploadVideoForUser(
            HttpServletRequest request,
            long userId,
            InputStream videoInputStream,
            String originalFileName,
            double startTimeSeconds,
            double endTimeSeconds,
            String videoTitle) throws IOException {
        log.debug("uploadVideoForUser, attempting to upload video for user with ID {}", userId);

        Optional<User> userOpt = userRepository.findById(userId);
        if(userOpt.isEmpty()) {
            log.error("uploadVideoForUser, user with ID {} not found", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find user");
        }

        Optional<VideoUploadUser> videoUser = videoUploadUserRepository.findVideoUploadUserByUsername(userOpt.get().getUsername());
        if(videoUser.isEmpty()) {
            log.error("uploadVideoForUser, video upload user for username {} not found", userOpt.get().getUsername());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find video upload user");
        }

        //Sanity checks on the input
        if (videoInputStream == null || videoInputStream.available() == 0) {
            log.error("uploadVideoForUser, video input stream is empty for user {}", userOpt.get().getUsername());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video input stream is empty");
        }

        if (originalFileName == null || originalFileName.isEmpty()) {
            log.error("uploadVideoForUser, original file name is empty for user {}", userOpt.get().getUsername());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Original file name is empty");
        }

        if (videoTitle == null || videoTitle.isEmpty()) {
            log.error("uploadVideoForUser, video title is empty for user {}", userOpt.get().getUsername());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video title is empty");
        }

        if (startTimeSeconds < 0 || endTimeSeconds < startTimeSeconds) {
            log.error("uploadVideoForUser, invalid start or end time for user {}", userOpt.get().getUsername());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid start or end time");
        }

        //Check if we can parse the file type
        MediaType fileType = Utils.getVideoTypeFromFileName(originalFileName);
        log.debug("uploadVideoForUser, uploading video for user {} with file type {}", userOpt.get().getUsername(),
                fileType);

        //Create new file name
        String newFileName = tryCreateFileName(videoUser.get());

        //Upload input video file
        String originalFileType = originalFileName.split("\\.")[originalFileName.split("\\.").length - 1];
        String inputFile = mediaManagementService.storeTempFile(videoInputStream, userOpt.get().getUsername(), newFileName.split("\\.")[0], originalFileType);

        //Process video upload asynchronously
        mediaManagementService.processVideoAsync(inputFile, userOpt.get().getUsername(), newFileName, startTimeSeconds, endTimeSeconds);

        // Save the video file to the repository
        log.debug("uploadVideoForUser, storing video file for user {} with file name {} in database as PROCESSING", userOpt.get().getUsername(), newFileName);
        VideoUploadUserFile videoUploadUserFile = new VideoUploadUserFile();
        videoUploadUserFile.setVideoUploadUser(videoUser.get());
        videoUploadUserFile.setFileName(newFileName);
        videoUploadUserFile.setFileSize((long) videoInputStream.available());
        videoUploadUserFile.setVideoTitle(videoTitle);
        videoUploadUserFile.setCreatedAt(java.time.LocalDateTime.now());
        videoUploadUserFile.setUploadStatus(VideoUploadStatus.PROCESSING);
        videoUploadUserFileRepository.save(videoUploadUserFile);

        // Return the file name for further processing
        return RESTUtils.fetchURLFromRequest(request, userOpt.get().getUsername(), "v", newFileName, false);
    }

    /**
     * Finds or creates a video upload user for the given user.
     *
     * @param userId The user ID to find or create a video upload user for.
     * @return The video upload user for the given user.
     */
    public VideoUploadUser getOrCreateVideoUploadUser(Long userId) {
        log.debug("getOrCreateVideoUploadUser, getting or creating video upload user for user ID {}", userId);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.error("getOrCreateVideoUploadUser, user ID {} not found", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find user");
        }
        User user = userOpt.get();

        Optional<VideoUploadUser> videoUploadUserOpt = videoUploadUserRepository.findVideoUploadUserByUsername(user.getUsername());
        if (videoUploadUserOpt.isPresent()) {
            log.debug("getOrCreateVideoUploadUser, video upload user for user {} found", user.getUsername());
            return videoUploadUserOpt.get();
        }

        //Create and persist new video upload user
        log.debug("getOrCreateVideoUploadUser, creating new video upload user for user {}", user.getUsername());
        VideoUploadUser videoUploadUser = new VideoUploadUser(user);
        videoUploadUserRepository.save(videoUploadUser);
        return videoUploadUser;
    }

    public VideoUploadUser saveVideoUploadUser(VideoUploadUser videoUploadUser) {
        log.debug("updateVideoUploadUser, updating video upload user with ID {}", videoUploadUser.getId());
        return videoUploadUserRepository.save(videoUploadUser);
    }

    public List<VideoUploadUserFile> getVideos(int page, int size, String username) {
        log.debug("getVideos, fetching videos for user {} with page {} and size {}", username, page, size);
        Optional<VideoUploadUser> videoUploadUserOpt = videoUploadUserRepository.findVideoUploadUserByUsername(username);
        if (videoUploadUserOpt.isEmpty()) {
            log.error("getVideos, video upload user for user {} not found", username);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find video upload user");
        }

        PageRequest request = PageRequest.of(page, size);
        return videoUploadUserFileRepository.findAllByVideoUploadUser(videoUploadUserOpt.get(), request).getContent();
    }

    /**
     * Attempts to create a file name for the user's given strategy. Aborts with a 500 error after 100 failed attempts.
     *
     * @return The file name.
     */
    private String tryCreateFileName(VideoUploadUser videoUploadUser) {
        for (int i = 0; i < 100; i++) {
            String fileName = Utils.generateFilenameFromStrategy(videoUploadUser.getVideoUploadMode()) + ".mp4";
            if (!videoUploadUserFileRepository.existsByVideoUploadUserAndFileName(videoUploadUser, fileName)) {
                log.debug("tryCreateFileName, determined file name {} for image hosting user {}", fileName, videoUploadUser.getId());
                return fileName;
            }
        }
        log.error("tryCreateFileName, could not create file name for user {} with strategy {}",
                videoUploadUser.getUser().getUsername(), videoUploadUser.getVideoUploadMode());
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create file for user.");
    }

    private boolean checkVideoExists(String username, String filename) {
        Optional<VideoUploadUser> videoUploadUserOpt = videoUploadUserRepository.findVideoUploadUserByUsername(username);
        if (videoUploadUserOpt.isEmpty()) {
            log.error("checkVideoExists, video upload user for user {} not found", username);
            return false;
        }
        //Check if this user has a file with the given filename
        if (!videoUploadUserFileRepository.existsByVideoUploadUserAndFileNameAndUploadStatus(videoUploadUserOpt.get(), filename, VideoUploadStatus.COMPLETED)) {
            log.error("checkVideoExists, video upload user for user {} does not have an uploaded file with filename {}", username, filename);
            return false;
        }
        return true;
    }
}

package gg.nya.imagehosting.services;

import gg.nya.imagehosting.models.*;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for handling video hosting operations, including uploading and retrieving videos.
 */
@Service
public class VideoHostingService {

    private static final Logger log = LoggerFactory.getLogger(VideoHostingService.class);

    private final VideoUploadUserRepository videoUploadUserRepository;
    private final VideoUploadUserFileRepository videoUploadUserFileRepository;
    private final S3Service s3Service;
    private final DataStorageService dataStorageService;
    private final UserService userService;

    /**
     * Constructor for VideoHostingService.
     * Initializes required repositories and services.
     *
     * @param videoUploadUserRepository Repository for retrieving users that can upload video.
     * @param dataStorageService Service for managing temporary and thumbnail storage on disk.
     * @param userFileRepository Repository for retrieving video files uploaded by users.
     * @param s3Service Service for interacting with S3 storage to retrieve and store video.
     * @param userService Service for managing user information, in case a new video upload user needs to be created.
     */
    @Autowired
    public VideoHostingService(VideoUploadUserRepository videoUploadUserRepository, DataStorageService dataStorageService,
                               VideoUploadUserFileRepository userFileRepository, S3Service s3Service, UserService userService) {
        this.videoUploadUserRepository = videoUploadUserRepository;
        this.dataStorageService = dataStorageService;
        this.videoUploadUserFileRepository = userFileRepository;
        this.s3Service = s3Service;
        this.userService = userService;
    }

    /**
     * Serve a video from the given user.
     *
     * @param username The username of the user.
     * @param filename The filename of the video.
     * @param start The start byte of the video to serve.
     * @param end The end byte of the video to serve.
     * @return The video as an input stream, if it exists. Throws a 404 error if the video does not exist.
     */
    public InputStream retrieveVideo(String username, String filename, long start, long end) {
        log.debug("retrieveVideo, retrieving video {} for user {}", filename, username);

        if (isVideoInvalid(username, filename)) {
            log.error("retrieveVideo, video for user {} with filename {} not found", username, filename);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found");
        }

        //Defer to S3 service and directly serve the returned input stream
        return s3Service.getFileStreamRange(username, filename, start, end);
    }

    /**
     * Uploads a video for the user associated with the given username.
     *
     * @param request   The HTTP request, used to construct the URL for the uploaded video.
     * @param username The username of the user for whom the video is being uploaded.
     * @param videoInputStream The InputStream containing the video data to be uploaded.
     * @param originalFileName The original file name of the video being uploaded.
     * @param startTimeSeconds The start time in seconds for the video segment, if applicable.
     * @param endTimeSeconds The end time in seconds for the video segment, if applicable.
     * @param videoTitle The title of the video being uploaded.
     * @return The URL of the uploaded video file.
     */
    public String uploadVideoForUser(
            HttpServletRequest request,
            String username,
            InputStream videoInputStream,
            String originalFileName,
            double startTimeSeconds,
            double endTimeSeconds,
            String videoTitle) {
        log.debug("uploadVideoForUser, attempting to upload video for user {}", username);

        Optional<VideoUploadUser> videoUserOpt = videoUploadUserRepository.findVideoUploadUserByUsername(username);
        if(videoUserOpt.isEmpty()) {
            log.error("uploadVideoForUser, video upload user for username {} not found", username);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find video upload user");
        }
        VideoUploadUser videoUser = videoUserOpt.get();

        //Sanity checks on the input
        //Check if the input stream is valid and non-empty
        final long videoInputStreamAvailable;
        try {
            videoInputStreamAvailable = videoInputStream.available();
            if (videoInputStreamAvailable == 0) {
                log.error("uploadVideoForUser, video input stream is empty for user {}", username);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video input stream is empty");
            }
        }
        catch (IOException e) {
            //No cleanup required - nothing was created yet
            log.error("uploadVideoForUser, error checking video input stream for user {}", username, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error checking video input stream");
        }

        // Check if the file has an original name
        if (originalFileName == null || originalFileName.isEmpty()) {
            log.error("uploadVideoForUser, file name is empty for user {}", username);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File name is empty");
        }

        // Check if the user provided a title
        if (videoTitle == null || videoTitle.isEmpty()) {
            log.error("uploadVideoForUser, video title is empty for user {}", username);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video title is empty");
        }

        // Check if the start and end times are valid
        if (startTimeSeconds < 0 || endTimeSeconds < startTimeSeconds) {
            log.error("uploadVideoForUser, invalid start ({}) or end ({}) time for user {}", startTimeSeconds, endTimeSeconds, username);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid start or end time");
        }

        //Check if we can parse the file type - throws a 400 error if not
        MediaType fileType = Utils.getVideoTypeFromFileName(originalFileName);
        log.debug("uploadVideoForUser, uploading video for user {} with file type {}", username, fileType);

        //Create new file name
        String newFileName = tryCreateFileName(videoUser);

        //Upload input video file
        String originalFileType = originalFileName.split("\\.")[originalFileName.split("\\.").length - 1]; //e.g. "mp4"
        String newFileNameWithoutExtension = newFileName.replaceAll("\\.[^.]+$", ""); //e.g. "abcd1234"
        String inputFile = username + "_" + newFileNameWithoutExtension + "_input." + originalFileName; //e.g. "myusername_abcd1234_input.mp4"
        log.debug("uploadVideoForUser, storing temporary input video file for user {} with file name {}", username, inputFile);
        Path tempInputFile = dataStorageService.storeTempFile(videoInputStream, inputFile);

        //Process video upload asynchronously
        processVideoAsync(tempInputFile, username, newFileName, startTimeSeconds, endTimeSeconds);

        // Save the video file to the repository as processing
        storeVideoInDatabase(username, videoTitle, newFileName, videoUser, videoInputStreamAvailable);

        // Return the URL of the uploaded video
        String videoUrl = RESTUtils.fetchURLFromRequest(request, username, "v", newFileName, false);
        log.debug("uploadVideoForUser, video upload initiated at URL {}", videoUrl);
        return videoUrl;  
    }

    /**
     * Finds or creates a video upload user entity for the given user.
     *
     * @param username The username of the user.
     * @return The video upload user entity for the given user.
     */
    public VideoUploadUser getOrCreateVideoUploadUser(String username) {
        log.debug("getOrCreateVideoUploadUser, getting or creating video upload user for user {}", username);

        Optional<VideoUploadUser> videoUploadUserOpt = videoUploadUserRepository.findVideoUploadUserByUsername(username);
        if (videoUploadUserOpt.isPresent()) {
            log.trace("getOrCreateVideoUploadUser, video upload user for user {} found", username);
            return videoUploadUserOpt.get();
        }

        //Create and persist new video upload user
        log.debug("getOrCreateVideoUploadUser, creating new video upload user for user {}", username);
        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            log.error("getOrCreateVideoUploadUser, user with username {} not found", username);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        
        VideoUploadUser videoUploadUser = new VideoUploadUser(userOpt.get());
        videoUploadUserRepository.save(videoUploadUser);
        return videoUploadUser;
    }

    /**
     * Get the length of a video in bytes from S3, if it exists.
     * @param username The username of the user.
     * @param filename The filename of the video.
     * @return The length of the video in bytes.
     */
    public long getVideoLength(String username, String filename) {
        log.debug("getVideoLength, fetching video length for user {} with filename {}", username, filename);
        if (isVideoInvalid(username, filename)) {
            log.error("getVideoLength, video for user {} with filename {} not found", username, filename);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found");
        }
        return s3Service.getFileSize(username, filename);
    }

    /**
     * Sets the user's preferred upload type and persists it to the database.
     * @param videoUploadUser The video upload user entity.
     * @param uploadType The preferred upload type to set.
     */
    public void setUserUploadTypePreference(VideoUploadUser videoUploadUser, HostingMode uploadType)
    {
        log.debug("setUserUploadTypePreference, setting upload type preference for upload user ID {} to {}", videoUploadUser.getId(), uploadType);
        Optional<VideoUploadUser> videoUploadUserOpt = videoUploadUserRepository.findById(videoUploadUser.getId());
        if (videoUploadUserOpt.isEmpty()) {
            log.error("setUserUploadTypePreference, video upload user with ID {} not found", videoUploadUser.getId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Video upload user not found");
        }
        videoUploadUserOpt.get().setVideoUploadMode(uploadType);
        videoUploadUserRepository.save(videoUploadUserOpt.get());

    }

    /**
     * Gets a paginated list of videos for the given user.
     *
     * @param page The page number to retrieve (0-indexed).
     * @param size The number of videos per page.
     * @param username The username of the user.
     * @return A list of videos for the given user.
     */
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
     * @param videoUploadUser The video upload user entity.
     * @return The generated file name.
     */
    private String tryCreateFileName(VideoUploadUser videoUploadUser) {
        for (int i = 0; i < 100; i++) {
            String fileName = Utils.generateFilenameFromStrategy(videoUploadUser.getVideoUploadMode()) + ".mp4";
            if (isVideoInvalid(videoUploadUser, fileName)) { //Invalid = file does not exist (yet)
                log.trace("tryCreateFileName, determined file name {} for image hosting user {}", fileName, videoUploadUser.getId());
                return fileName;
            }
        }
        log.error("tryCreateFileName, could not create file name for user {} with strategy {}",
                videoUploadUser.getUser().getUsername(), videoUploadUser.getVideoUploadMode());
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create file for user.");
    }

    /**
     * Queries the database to check if the user with the given username has a video with the given filename.
     *
     * @param username The username of the user.
     * @param filename The filename of the video.
     * @return True if the video doesn't exist, false otherwise.
     */
    private boolean isVideoInvalid(String username, String filename) {
        Optional<VideoUploadUser> videoUploadUserOpt = videoUploadUserRepository.findVideoUploadUserByUsername(username);
        if (videoUploadUserOpt.isEmpty()) {
            log.trace("checkVideoExists, video upload user for user {} not found", username);
            return true;
        }
        return isVideoInvalid(videoUploadUserOpt.get(), filename);
    }

    /**
     * Queries the database to check if the user has a video with the given filename.
     *
     * @param videoUploadUser The video upload user entity.
     * @param filename The filename of the video.
     * @return True if the video doesn't exist, false otherwise.
     */
    private boolean isVideoInvalid(VideoUploadUser videoUploadUser, String filename) {
        //Check if this user has a file with the given filename
        if (!videoUploadUserFileRepository.existsByVideoUploadUserAndFileNameAndUploadStatus(
                videoUploadUser, filename, VideoUploadStatus.COMPLETED)) {
            log.trace("checkVideoExists, video upload user for user {} does not have an uploaded file with filename {}",
                    videoUploadUser.getUser().getUsername(), filename);
            return true;
        }
        return false;
    }

    /**
     * Asynchronously processes the video and uploads the result to S3.
     *
     * @param inputFileName    The  of the input video file.
     * @param username         The username of the user who uploaded the video.
     * @param outputFileName   The name of the output video file.
     * @param startTimeSeconds The start time in seconds for the video segment to process.
     * @param endTimeSeconds   The end time in seconds for the video segment to process.
     * @throws RuntimeException if an error occurs during video processing.
     */
    @Async
    private void processVideoAsync(String inputFileName, String username, String outputFileName,
                                  double startTimeSeconds, double endTimeSeconds) {
        log.debug("processVideoAsync, starting async video processing for user {} file {}", username, outputFileName);

        try {
            String processedVideoFileName = "output_" + username + "_" + outputFileName;

            log.debug("processVideoAsync, converting video file: {}", inputFileName);
            convertToMp4(inputFileName, processedVideoFileName, startTimeSeconds, endTimeSeconds);

            log.debug("processVideoAsync, generating thumbnail for user {} file {}", username, outputFileName);
            String thumbnailFileName = "thumbnail_" + username + "_" + outputFileName.replaceAll("\\.[^.]+$", ".png");
            generateThumbnail(processedVideoFileName, thumbnailFileName);

            log.debug("processVideoAsync, storing thumbnail for user {} file {}", username, outputFileName);

            InputStream thumbnailInputStream = createThumbnailInputStream(thumbnailFileName);
            dataStorageService.storeThumbnail(username, outputFileName, thumbnailInputStream);
            thumbnailInputStream.close();


            log.debug("processVideoAsync, uploading video to S3 for user {}", username);
            try {
                InputStream videoInputStream = new FileInputStream(tempDirectory + "/" + processedVideoFileName);
                s3Service.uploadVideo(username, outputFileName, videoInputStream);
            }
            catch (IOException e) {
                log.error("processVideoAsync, failed to read video file for user {} file {}", username, outputFileName, e);
                throw new RuntimeException("Failed to read video file", e);
            }

            log.debug("processVideoAsync, video processing completed for user {} file {}", username, outputFileName);
            updateDatabaseStatus(username, outputFileName, VideoUploadStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Failed to process video for user {} file {}", username, outputFileName, e);
            updateDatabaseStatus(username, outputFileName, VideoUploadStatus.FAILED);
            throw e;
        }
    }

    /**
     * Convert a video file to MP4 format with the same resolution as input.
     *
     * @param originalFilename The original filename to convert from.
     * @param newFileName The name of the new file to be created.
     * @param startDurationSeconds Start time in seconds for the video segment
     * @param endDurationSeconds End time in seconds for the video segment
     */
    public void convertToMp4(String originalFilename, String newFileName,
                             double startDurationSeconds, double endDurationSeconds) {
        try {
            final long startTimeMs = (long) (startDurationSeconds * 1000);
            final long endTimeMs = (long) (endDurationSeconds * 1000);

            log.debug("convertToMp4, converting video {} to mp4 {} and clipping to ({}, {})",
                    originalFilename, newFileName, startDurationSeconds, endDurationSeconds);

            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(tempDirectory + "/" + originalFilename)
                    .overrideOutputFiles(true)
                    .addOutput(tempDirectory + "/" + newFileName)
                    .setFormat("mp4")
                    .setVideoCodec("libx264")
                    .setAudioCodec("aac")
                    .setStartOffset(startTimeMs, TimeUnit.MILLISECONDS)
                    .setDuration(endTimeMs - startTimeMs, TimeUnit.MILLISECONDS)
                    .addExtraArgs("-preset", "fast")
                    .addExtraArgs("-crf", "23")
                    .addExtraArgs("-threads", "0")
                    .done();

            // Execute conversion
            executor.createJob(builder).run();

            log.debug("convertToMp4, video conversion completed successfully for file {}", newFileName);
        } catch (Exception e) {
            log.error("convertToMp4, failed to convert video to MP4", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to convert video to MP4", e);
        }
    }

    /**
     * Generates a thumbnail from the first frame of a video file at 480p resolution.
     *
     * @param videoFileName The name of the video file to extract thumbnail from
     * @param thumbnailFileName The name of the thumbnail file to create
     * @throws RuntimeException if an error occurs during thumbnail generation
     */
    private void generateThumbnail(String videoFileName, String thumbnailFileName) {
        try {
            log.debug("generateThumbnail, generating thumbnail from video {} to {}", videoFileName, thumbnailFileName);

            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(tempDirectory + "/" + videoFileName)
                    .overrideOutputFiles(true)
                    .addOutput(tempDirectory + "/" + thumbnailFileName)
                    .setFrames(1)
                    .setVideoFilter("scale=480:270")
                    .done();

            executor.createJob(builder).run();

            log.debug("generateThumbnail, thumbnail generation completed successfully for file {}", thumbnailFileName);
        } catch (Exception e) {
            log.error("generateThumbnail, failed to generate thumbnail from video {}", videoFileName, e);
            throw new RuntimeException("Failed to generate thumbnail", e);
        }
    }

    /**
     * Creates an InputStream for reading a thumbnail file from the temp directory.
     *
     * @param thumbnailFileName The name of the thumbnail file to read
     * @return InputStream for the thumbnail file
     * @throws RuntimeException if an error occurs while reading the thumbnail
     */
    private InputStream createThumbnailInputStream(String thumbnailFileName) {
        try {
            Path thumbnailPath = Path.of(tempDirectory, thumbnailFileName);
            log.debug("createThumbnailInputStream, creating input stream for thumbnail {}", thumbnailPath);
            return new FileInputStream(thumbnailPath.toFile());
        } catch (IOException e) {
            log.error("createThumbnailInputStream, failed to create input stream for thumbnail {}", thumbnailFileName, e);
            throw new RuntimeException("Failed to create thumbnail input stream", e);
        }
    }

    /**
     * Update the status of the file in the database.
     * @param username the username of the user who uploaded the file
     * @param fileName the name of the file to update
     * @param status the new status of the file (e.g., "PROCESSING", "COMPLETED", "FAILED")
     */
    private void updateDatabaseStatus(String username, String fileName, VideoUploadStatus status) {
        Optional<VideoUploadUserFile> videoFileOpt = videoUploadUserFileRepository.getVideoUploadUserFileByUploadUsernameAndFileName(username, fileName);
        if (videoFileOpt.isPresent()) {
            VideoUploadUserFile videoFile = videoFileOpt.get();
            videoFile.setUploadStatus(status);
            videoUploadUserFileRepository.save(videoFile);
        }
        else {
            log.warn("updateDatabaseStatus, failed to update status for file {} for user {}: file not found in database",
                    fileName, username);
        }
    }
    
    private void storeVideoInDatabase(String username, String videoTitle, String newFileName, VideoUploadUser videoUser, long videoInputStreamAvailable) {
        log.debug("uploadVideoForUser, storing video file for user {} with file name {} in database as PROCESSING", username, newFileName);
        VideoUploadUserFile videoUploadUserFile = new VideoUploadUserFile();
        videoUploadUserFile.setVideoUploadUser(videoUser);
        videoUploadUserFile.setFileName(newFileName);
        videoUploadUserFile.setFileSize(videoInputStreamAvailable); //TODO check if this actually works?
        videoUploadUserFile.setVideoTitle(videoTitle);
        videoUploadUserFile.setCreatedAt(LocalDateTime.now());
        videoUploadUserFile.setUploadStatus(VideoUploadStatus.PROCESSING);
        videoUploadUserFileRepository.save(videoUploadUserFile);
    }

}

package gg.nya.imagehosting.services;

import gg.nya.imagehosting.models.*;
import gg.nya.imagehosting.repositories.VideoUploadUserFileRepository;
import gg.nya.imagehosting.repositories.VideoUploadUserRepository;
import gg.nya.imagehosting.utils.RESTUtils;
import gg.nya.imagehosting.utils.Utils;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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

    @Value("${media.ffmpeg.path}")
    private String ffmpegPath;
    @Value("${media.ffprobe.path}")
    private String ffprobePath;

    private final ApplicationContext applicationContext;
    private FFmpegExecutor executor;

    /**
     * Constructor for VideoHostingService.
     * Initializes required repositories and services.
     *
     * @param videoUploadUserRepository Repository for retrieving users that can upload video.
     * @param dataStorageService Service for managing temporary and thumbnail storage on disk.
     * @param userFileRepository Repository for retrieving video files uploaded by users.
     * @param s3Service Service for interacting with S3 storage to retrieve and store video.
     * @param userService Service for managing user information, in case a new video upload user needs to be created.
     * @param applicationContext The Spring application context, used to get a proxy of this service for async processing.
     */
    @Autowired
    public VideoHostingService(VideoUploadUserRepository videoUploadUserRepository, DataStorageService dataStorageService,
                               VideoUploadUserFileRepository userFileRepository, S3Service s3Service, UserService userService,
                               ApplicationContext applicationContext) {
        this.videoUploadUserRepository = videoUploadUserRepository;
        this.dataStorageService = dataStorageService;
        this.videoUploadUserFileRepository = userFileRepository;
        this.s3Service = s3Service;
        this.userService = userService;
        this.applicationContext = applicationContext;
    }

    /**
     * Initializes the FFmpeg executor with the configured paths to the FFmpeg and FFprobe binaries.
     * @throws IOException if the binaries cannot be found or accessed.
     */
    @PostConstruct
    public void init() throws IOException {
        FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
        FFprobe ffprobe = new FFprobe(ffprobePath);
        this.executor = new FFmpegExecutor(ffmpeg, ffprobe);
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
        return s3Service.getFileStreamRange(username, filename + ".mp4", start, end);
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

        //Create new file name - function returns name without extension
        String newFileName = tryCreateFileName(videoUser);

        //Upload input video file
        String originalFileType = fileType.getSubtype(); //e.g. "mp4"
        String inputFile = username + "_" + newFileName + "_input." + originalFileType; //e.g. "myusername_abcd1234_input.mp4"
        log.debug("uploadVideoForUser, storing temporary input video file for user {} with file name {}", username, inputFile);

        dataStorageService.storeTempFile(videoInputStream, inputFile);

        // Save the video file to the repository as processing
        storeVideoInDatabase(username, videoTitle, newFileName, videoUser, videoInputStreamAvailable);

        //Process video upload asynchronously - call via proxy for async to work
        applicationContext.getBean(VideoHostingService.class).processVideoAsync(
                newFileName, fileType, username, startTimeSeconds, endTimeSeconds);

        // Return the URL of the uploaded video
        String videoUrl = RESTUtils.fetchURLFromRequest(request, username, "v", newFileName);
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
        return s3Service.getFileSize(username, filename + ".mp4");
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
     * Gets a paginated list of videos for the given user. Videos are sorted by creation date in descending order.
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

        PageRequest request = PageRequest.of(page, size)
                .withSort(Sort.by(Sort.Direction.DESC, "createdAt"));
        return videoUploadUserFileRepository.findAllByVideoUploadUserAndUploadStatus(videoUploadUserOpt.get(), VideoUploadStatus.COMPLETED, request).getContent();
    }

    /**
     * Fetches video metadata for the given user and filename.
     * @param username The username of the user.
     * @param filename The filename of the video.
     * @return An Optional containing the video metadata if found, or empty if not found.
     */
    public Optional<VideoUploadUserFile> getVideoData(String username, String filename) {
        log.debug("getVideoData, fetching video data for user {} with filename {}", username, filename);
        return videoUploadUserFileRepository.getVideoUploadUserFileByUploadUsernameAndFileName(username, filename);
    }

    /**
     * Attempts to create a file name for the user's given strategy. Aborts with a 500 error after 100 failed attempts.
     *
     * @param videoUploadUser The video upload user entity.
     * @return The generated file name, without extension.
     */
    private String tryCreateFileName(VideoUploadUser videoUploadUser) {
        for (int i = 0; i < 100; i++) {
            String fileName = Utils.generateFilenameFromStrategy(videoUploadUser.getVideoUploadMode());
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
            log.trace("isVideoInvalid, video upload user for user {} does not have an uploaded file with filename {}",
                    videoUploadUser.getUser().getUsername(), filename);
            return true;
        }
        return false;
    }

    /**
     * Asynchronously processes the video and uploads the result to S3.
     *
     * @param filename         The base filename (without extension) for the processed video.
     * @param originalFileType The original file type of the uploaded video.
     * @param username         The username of the user who uploaded the video - to determine S3 bucket path.
     * @param startTimeSeconds The start time in seconds for the video segment to process.
     * @param endTimeSeconds   The end time in seconds for the video segment to process.
     */
    @Async
    protected void processVideoAsync(String filename, MediaType originalFileType, String username,
                                     double startTimeSeconds, double endTimeSeconds) {
        Path inputFilePath = dataStorageService.generateTempPath(username + "_" + filename + "_input." + originalFileType.getSubtype());
        Path outputFilePath = dataStorageService.generateTempPath(username + "_" + filename + ".mp4");
        Path thumbnailPath = dataStorageService.generateTempPath("thumbnail_" + username + "_" + filename + ".png");

        log.debug("processVideoAsync, converting video file: {} -> {}", inputFilePath, outputFilePath);
        convertToMp4(inputFilePath, outputFilePath, startTimeSeconds, endTimeSeconds);

        log.debug("processVideoAsync, generating thumbnail for user {} file {}", username, filename);
        generateThumbnail(inputFilePath, thumbnailPath);

        log.debug("processVideoAsync, copying thumbnail {} to final location", thumbnailPath);
        try {
            InputStream thumbnailInputStream = new FileInputStream(thumbnailPath.toFile());
            dataStorageService.storeThumbnail(username, "v-" + filename, thumbnailInputStream);
            thumbnailInputStream.close();
        }
        catch(IOException e) {
            log.error("processVideoAsync, failed to write thumbnail file {} for user {}", thumbnailPath, username, e);
            cleanUpUpload(username, filename, List.of(inputFilePath, outputFilePath, thumbnailPath), VideoUploadStatus.FAILED);
            return;
        }

        log.debug("processVideoAsync, uploading video {} to S3 for user {}", outputFilePath, username);
        try {
            InputStream videoInputStream = new FileInputStream(outputFilePath.toFile());
            s3Service.uploadVideo(username, filename + ".mp4", videoInputStream);
        }
        catch (IOException e) {
            log.error("processVideoAsync, failed to store video file {} to S3", outputFilePath, e);
            cleanUpUpload(username, filename, List.of(inputFilePath, outputFilePath, thumbnailPath), VideoUploadStatus.FAILED);
            return;
        }

        log.debug("processVideoAsync, video processing completed for user {} file {}", username, filename);
        cleanUpUpload(username, filename, List.of(inputFilePath, outputFilePath, thumbnailPath), VideoUploadStatus.COMPLETED);
    }

    /**
     * Cleans up temporary files and updates the database status to the specified status.
     *
     * @param username    the username of the user who uploaded the file
     * @param newFileName the name of the file
     * @param tempFiles   list of temporary files to delete
     * @param status      the status to set in the database (e.g., "COMPLETED", "FAILED")
     */
    public void cleanUpUpload(String username, String newFileName, List<Path> tempFiles, VideoUploadStatus status) {
        log.debug("cleanUpUpload, cleaning up upload for user {} with file name {}, status {}", username, newFileName, status);
        //Delete temp input file
        for(Path tempFile : tempFiles) {
            try {
                log.trace("cleanUpUpload, deleting temporary file {} for user {}", tempFile, username);
                dataStorageService.removeTempFile(tempFile.toString());
            } catch (Exception e) {
                log.error("cleanUpUpload, failed to delete temporary file {} for user {}", tempFile, username, e);
            }
        }

        //Update database status
        updateDatabaseStatus(username, newFileName, status);
    }

    /**
     * Convert a video file to MP4 format with the same resolution as input.
     *
     * @param inputFilePath The original path to the file to convert from.
     * @param outputFilePath The output path to the file to convert to.
     * @param startDurationSeconds Start time in seconds for the video segment
     * @param endDurationSeconds End time in seconds for the video segment
     */
    private void convertToMp4(Path inputFilePath, Path outputFilePath, double startDurationSeconds,
                             double endDurationSeconds) {
        final long startTimeMs = (long) (startDurationSeconds * 1000);
        final long endTimeMs = (long) (endDurationSeconds * 1000);

        log.trace("convertToMp4, converting video {} to {} and clipping to ({}, {})",
                inputFilePath, outputFilePath, startDurationSeconds, endDurationSeconds);

        long startTimeExecution = System.currentTimeMillis();

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(inputFilePath.toString())
                .overrideOutputFiles(true)
                .addOutput(outputFilePath.toString())
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

        long endTimeExecution = System.currentTimeMillis();
        long executionMs = endTimeExecution - startTimeExecution;
        log.debug("convertToMp4, conversion completed successfully to file {} in {}ms", outputFilePath, executionMs);
    }

    /**
     * Generates a thumbnail from the first frame of a video file at 480p resolution.
     *
     * @param inputFilePath The original path to the file to convert from.
     * @param thumbnailFilePath The path at which to save the generated thumbnail.
     */
    private void generateThumbnail(Path inputFilePath, Path thumbnailFilePath) {
        log.trace("generateThumbnail, generating thumbnail from video {} to {}", inputFilePath, thumbnailFilePath);

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(inputFilePath.toString())
                .overrideOutputFiles(true)
                .addOutput(thumbnailFilePath.toString())
                .setFrames(1)
                .setVideoFilter("scale=480:270")
                .done();
        executor.createJob(builder).run();

        log.debug("generateThumbnail, thumbnail generation completed successfully for file {}", thumbnailFilePath);
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

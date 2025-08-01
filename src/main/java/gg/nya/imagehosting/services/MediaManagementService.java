package gg.nya.imagehosting.services;

import gg.nya.imagehosting.models.VideoUploadStatus;
import gg.nya.imagehosting.models.VideoUploadUserFile;
import gg.nya.imagehosting.repositories.VideoUploadUserFileRepository;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import jakarta.annotation.PostConstruct;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class MediaManagementService {

    private static final Logger log = LoggerFactory.getLogger(MediaManagementService.class);
    private final S3Service s3Service;
    private final VideoUploadUserFileRepository videoUploadUserFileRepository;
    private final StaticDataService staticDataService;

    @Value("${media.ffmpeg.path}")
    private String ffmpegPath;

    @Value("${media.ffprobe.path}")
    private String ffprobePath;

    @Value("${media.temp.directory}")
    private String tempDirectory;

    private FFmpeg ffmpeg;
    private FFprobe ffprobe;
    private FFmpegExecutor executor;

    public MediaManagementService(S3Service s3Service, VideoUploadUserFileRepository videoUploadUserFileRepository, StaticDataService staticDataService) {
        this.s3Service = s3Service;
        this.videoUploadUserFileRepository = videoUploadUserFileRepository;
        this.staticDataService = staticDataService;
    }

    @PostConstruct
    public void init() throws IOException {
        this.ffmpeg = new FFmpeg(ffmpegPath);
        this.ffprobe = new FFprobe(ffprobePath);
        this.executor = new FFmpegExecutor(ffmpeg, ffprobe);
    }

    /**
     * Store a temporary file in the specified directory.
     * @param inputStream The input stream of the file to be stored.
     * @param user The user identifier for whom the file is being stored.
     * @param fileName The name of the file to be stored.
     * @param fileExtension The file extension of the file to be stored.
     * @return The path to the stored temporary file as a string.
     * @throws RuntimeException if an error occurs while storing the file.
     */
    public String storeTempFile(InputStream inputStream, String user, String fileName, String fileExtension) {
        final Path tempInputFile;
        try {
            String filename = "input_" + user + "_" + fileName;
            Path tempDir = Path.of(tempDirectory);
            Files.createDirectories(tempDir);
            tempInputFile = Files.createFile(Path.of(tempDir + "/" + filename + "." + fileExtension));
            Files.copy(inputStream, tempInputFile, StandardCopyOption.REPLACE_EXISTING);
            log.debug("storeTempFile, uploaded file {} for user {} to temporary location: {}", fileName, user, tempInputFile);
            return filename + "." + fileExtension;
        } catch (IOException e) {
            log.error("storeTempFile, failed to upload file {} for user {}", fileName, user, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Asynchronously processes the video and uploads the result to S3.
     *
     * @param inputFileName    The name of the input video file.
     * @param username         The username of the user who uploaded the video.
     * @param outputFileName   The name of the output video file.
     * @param startTimeSeconds The start time in seconds for the video segment to process.
     * @param endTimeSeconds   The end time in seconds for the video segment to process.
     * @throws RuntimeException if an error occurs during video processing.
     */
    @Async
    public void processVideoAsync(String inputFileName, String username, String outputFileName,
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
            try {
                InputStream thumbnailInputStream = createThumbnailInputStream(thumbnailFileName);
                String baseFileName = outputFileName.replaceAll("\\.[^.]+$", "");
                staticDataService.storeThumbnail(username, baseFileName, thumbnailInputStream);
                thumbnailInputStream.close();
            } catch (IOException e) {
                log.error("processVideoAsync, failed to store thumbnail for user {} file {}", username, outputFileName, e);
            }

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
            
            log.debug("convertToMp4, converting video {} to webm {} and clipping to ({}, {})",
                originalFilename, newFileName, startDurationSeconds, endDurationSeconds);

            FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(tempDirectory + "/" + originalFilename)
                .overrideOutputFiles(true)
                .addOutput(tempDirectory + "/" + newFileName)
                .setFormat("webm")
                .setVideoCodec("libvpx-vp9")
                .setAudioCodec("libopus")
                .setStartOffset(startTimeMs, TimeUnit.MILLISECONDS)
                .setDuration(endTimeMs - startTimeMs, TimeUnit.MILLISECONDS)
                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
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
}
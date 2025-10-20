package gg.nya.imagehosting.jobs;

import gg.nya.imagehosting.models.VideoUploadStatus;
import gg.nya.imagehosting.models.VideoUploadUserFile;
import gg.nya.imagehosting.services.DataStorageService;
import gg.nya.imagehosting.services.VideoHostingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that cleans up video uploads that have been processing for too long.
 */
@Component
public class FailedVideoUploadCleanupJob {
    private final VideoHostingService videoHostingService;
    private final DataStorageService dataStorageService;

    @Value("${app.jobs.video-cleanup.threshold-hours}")
    private int thresholdHours;
    @Value("${app.jobs.video-cleanup.batch-size}")
    private int batchSize;

    private final Logger log = LoggerFactory.getLogger(FailedVideoUploadCleanupJob.class);

    /**
     * Constructor for the FailedVideoUploadCleanupJob. Injects the relevant services.
     *
     * @param videoHostingService The video hosting service, to retrieve and clean up abandoned uploads.
     * @param dataStorageService The data storage service, to generate temporary file paths.
     */
    public FailedVideoUploadCleanupJob(VideoHostingService videoHostingService, DataStorageService dataStorageService) {
        this.videoHostingService = videoHostingService;
        this.dataStorageService = dataStorageService;
    }

    /**
     * Cleans up video uploads that have been processing for too long.
     */
    @Scheduled(fixedRateString = "${app.jobs.video-cleanup.schedule}")
    public void cleanupFailedUploads() {
        log.trace("cleanupFailedUploads, starting job at {}", LocalDateTime.now());

        // Retrieve a batch of abandoned video uploads
        List<VideoUploadUserFile> abandonedVideos = videoHostingService.getAbandonedVideos(thresholdHours, batchSize);
        log.trace("cleanupFailedUploads, found {} abandoned uploads to clean up", abandonedVideos.size());

        for(VideoUploadUserFile video : abandonedVideos) {
            String username = video.getVideoUploadUser().getUser().getUsername();
            String filename = video.getFileName();
            log.trace("cleanupFailedUploads, cleaning up abandoned upload: id={}, username={}, filename={}", video.getId(), username, filename);

            // TODO: Unfortunately with current design the input temp file can't be deleted since we don't know the original file type
            // See Issue #8
            videoHostingService.cleanUpUpload(
                    username,
                    filename,
                    List.of(
                        dataStorageService.getTempFilePath(username + "_" + filename + ".mp4"),
                        dataStorageService.getTempFilePath("thumbnail_" + username + "_" + filename + ".png")
                    ),
                    VideoUploadStatus.FAILED
            );
        }
    }
}

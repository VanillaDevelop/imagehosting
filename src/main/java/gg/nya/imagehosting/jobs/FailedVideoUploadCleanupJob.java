package gg.nya.imagehosting.jobs;

import gg.nya.imagehosting.models.VideoUploadStatus;
import gg.nya.imagehosting.models.VideoUploadUserFile;
import gg.nya.imagehosting.services.DataStorageService;
import gg.nya.imagehosting.services.VideoHostingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class FailedVideoUploadCleanupJob {
    private final VideoHostingService videoHostingService;
    private final DataStorageService dataStorageService;

    private final Logger log = LoggerFactory.getLogger(FailedVideoUploadCleanupJob.class);

    public FailedVideoUploadCleanupJob(VideoHostingService videoHostingService, DataStorageService dataStorageService) {
        this.videoHostingService = videoHostingService;
        this.dataStorageService = dataStorageService;
    }

    @Scheduled(fixedRate = 30 * 60 * 1000) // Every 30 minutes
    public void cleanupFailedUploads() {
        log.trace("cleanupFailedUploads, starting job at {}", LocalDateTime.now());

        // Retrieve up to 100 abandoned video uploads
        List<VideoUploadUserFile> abandonedVideos = videoHostingService.getAbandonedVideos(100);

        for(VideoUploadUserFile video : abandonedVideos) {
            String username = video.getVideoUploadUser().getUser().getUsername();
            String filename = video.getFileName();
            log.trace("cleanupFailedUploads, cleaning up abandoned upload: id={}, username={}, filename={}", video.getId(), username, filename);

            // Unfortunately with current design the input temp file can't be deleted since we don't know the original file type
            videoHostingService.cleanUpUpload(
                    username,
                    filename,
                    List.of(
                        dataStorageService.generateTempPath(username + "_" + filename + ".mp4"),
                        dataStorageService.generateTempPath("thumbnail_" + username + "_" + filename + ".png")
                    ),
                    VideoUploadStatus.FAILED
            );
        }
    }
}

package gg.nya.imagehosting.repositories;

import gg.nya.imagehosting.models.VideoUploadStatus;
import gg.nya.imagehosting.models.VideoUploadUser;
import gg.nya.imagehosting.models.VideoUploadUserFile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VideoUploadUserFileRepository extends PagingAndSortingRepository<VideoUploadUserFile, Long>,
        CrudRepository<VideoUploadUserFile, Long> {
    boolean existsByVideoUploadUserAndFileName(VideoUploadUser videoUploadUser, String fileName);
    boolean existsByVideoUploadUserAndFileNameAndUploadStatus(VideoUploadUser videoUploadUser, String fileName, VideoUploadStatus uploadStatus);

    @Query("SELECT vuf FROM video_upload_user_files vuf WHERE vuf.videoUploadUser.user.username = ?1 AND vuf.fileName = ?2")
    Optional<VideoUploadUserFile> getVideoUploadUserFileByUploadUsernameAndFileName(String username, String fileName);

    Slice<VideoUploadUserFile> findAllByVideoUploadUserAndUploadStatus(VideoUploadUser videoUploadUser, VideoUploadStatus uploadStatus, Pageable pageable);

    Slice<VideoUploadUserFile> findAllByUploadStatusAndCreatedAtBefore(VideoUploadStatus uploadStatus, LocalDateTime date, Pageable pageable);

}

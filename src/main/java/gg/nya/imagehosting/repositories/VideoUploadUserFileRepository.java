package gg.nya.imagehosting.repositories;

import gg.nya.imagehosting.models.VideoUploadUser;
import gg.nya.imagehosting.models.VideoUploadUserFile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoUploadUserFileRepository extends CrudRepository<VideoUploadUserFile, Long> {
    boolean existsByVideoUploadUserAndFileName(VideoUploadUser videoUploadUser, String fileName);
}

package gg.nya.imagehosting.repositories;

import gg.nya.imagehosting.models.VideoUploadUser;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoUploadUserRepository extends CrudRepository<VideoUploadUser, Long> {

    @Query("SELECT v FROM video_upload_users v WHERE v.user.username = :username")
    Optional<VideoUploadUser> findVideoUploadUserByUser(@Param("username") String username);
}

package gg.nya.imagehosting.repositories;

import gg.nya.imagehosting.models.ImageHostingUser;
import gg.nya.imagehosting.models.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImageHostingUserRepository extends CrudRepository<ImageHostingUser, Long> {
    Optional<ImageHostingUser> findImageHostingUserByUser(User user);

    Optional<ImageHostingUser> findImageHostingUserByApiKey(String apiKey);

    @Query("SELECT i FROM image_hosting_users i WHERE i.user.username = :username")
    Optional<ImageHostingUser> findImageHostingUserByUsername(@Param("username") String username);
}

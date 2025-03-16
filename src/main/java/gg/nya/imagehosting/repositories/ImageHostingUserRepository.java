package gg.nya.imagehosting.repositories;

import gg.nya.imagehosting.models.ImageHostingUser;
import gg.nya.imagehosting.models.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImageHostingUserRepository extends CrudRepository<ImageHostingUser, Long> {
    Optional<ImageHostingUser> findImageHostingUserByUser(User user);
}

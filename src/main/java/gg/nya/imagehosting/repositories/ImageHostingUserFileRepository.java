package gg.nya.imagehosting.repositories;

import gg.nya.imagehosting.models.ImageHostingUserFile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageHostingUserFileRepository extends CrudRepository<ImageHostingUserFile, Long> {
}

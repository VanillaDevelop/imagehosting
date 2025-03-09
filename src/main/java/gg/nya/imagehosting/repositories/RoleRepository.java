package gg.nya.imagehosting.repositories;

import gg.nya.imagehosting.models.Role;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends CrudRepository<Role, Long> {
    @Override
    @NonNull
    List<Role> findAll();

    @NonNull
    Optional<Role> findFirstByRole(@NonNull String role);
}

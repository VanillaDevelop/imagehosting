package gg.nya.imagehosting.repositories;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import gg.nya.imagehosting.models.Role;

@Repository
public interface RoleRepository extends CrudRepository<Role, Long> {
    @Override
    @NonNull
    List<Role> findAll();
}

package gg.nya.imagehosting.services;

import java.util.List;

import org.springframework.stereotype.Service;

import gg.nya.imagehosting.models.Role;
import gg.nya.imagehosting.repositories.RoleRepository;

@Service
public class RoleService {
    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<Role> getRoles() {
        return roleRepository.findAll();
    }
}

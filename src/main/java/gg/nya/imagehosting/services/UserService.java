package gg.nya.imagehosting.services;

import gg.nya.imagehosting.models.Role;
import gg.nya.imagehosting.models.User;
import gg.nya.imagehosting.repositories.UserRepository;
import gg.nya.imagehosting.security.CustomAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleService roleService;

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    public UserService(UserRepository userRepository, RoleService roleService) {
        this.userRepository = userRepository;
        this.roleService = roleService;
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public void createUser(User user) {
        userRepository.save(user);
    }

    public List<User> getUsers(int page, int size) {
        PageRequest request = PageRequest.of(page, size);
        return userRepository.findAll(request).getContent();
    }

    public long getUserCount() {
        return userRepository.count();
    }

    /**
     * Attempts to remove a role from a user.
     *
     * @param user The user to remove the role from.
     * @param role The name of the role to remove.
     * @return An optional error message if the role could not be removed.
     */
    public Optional<String> removeRoleFromUser(User user, String role) {
        Optional<User> updatedUserOpt = userRepository.findById(user.getId());
        if (updatedUserOpt.isEmpty()) {
            log.warn("removeRoleFromUser, user {} not found", user.getUsername());
            return Optional.of("User " + user.getUsername() + " not found");
        }
        User updatedUser = updatedUserOpt.get();

        log.info("removeRoleFromUser, removing role {} from user {}", role, updatedUser.getUsername());
        //Check if user has role
        if (!updatedUser.getRoles().stream().map(Role::getRole).toList().contains(role)) {
            log.warn("removeRoleFromUser, user {} does not have role {}", updatedUser.getUsername(), role);
            return Optional.of("User " + updatedUser.getUsername() + " does not have role " + role);
        }
        //Check user is not trying to remove his own admin role
        if (updatedUser.getRoles().stream().map(Role::getRole).toList().contains("ADMIN") && role.equals("ADMIN")
                && ((CustomAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getUserId().equals(updatedUser.getId())) {
            log.warn("removeRoleFromUser, user {} is trying to remove his own admin role", updatedUser.getUsername());
            return Optional.of("You cannot remove your own admin role");
        }
        //Try to remove role from user
        updatedUser.getRoles().removeIf(r -> r.getRole().equals(role));
        userRepository.save(updatedUser);
        log.info("removeRoleFromUser, role {} removed from user {}", role, updatedUser.getUsername());
        return Optional.empty();
    }

    /**
     * Attempts to add a role to a user.
     *
     * @param user The user to add the role to.
     * @param role The name of the role to add.
     * @return An optional error message if the role could not be added.
     */
    public Optional<String> addRoleToUser(User user, String role) {
        Optional<User> updatedUserOpt = userRepository.findById(user.getId());
        if (updatedUserOpt.isEmpty()) {
            log.warn("addRoleToUser, user {} not found", user.getUsername());
            return Optional.of("User " + user.getUsername() + " not found");
        }
        Optional<Role> roleToAddOpt = roleService.getRoleByName(role);
        if (roleToAddOpt.isEmpty()) {
            log.warn("addRoleToUser, role {} not found", role);
            return Optional.of("Role " + role + " not found");
        }
        User updatedUser = updatedUserOpt.get();
        Role roleToAdd = roleToAddOpt.get();

        log.info("addRoleToUser, adding role {} to user {}", roleToAdd.getRole(), updatedUser.getUsername());
        //Check if user already has role
        if (updatedUser.getRoles().stream().map(Role::getRole).toList().contains(roleToAdd.getRole())) {
            log.warn("addRoleToUser, user {} already has role {}", updatedUser.getUsername(), roleToAdd.getRole());
            return Optional.of("User " + updatedUser.getUsername() + " already has role " + roleToAdd.getRole());
        }
        //Try to add role to user
        updatedUser.getRoles().add(roleToAdd);
        userRepository.save(updatedUser);
        log.info("addRoleToUser, role {} added to user {}", roleToAdd.getRole(), updatedUser.getUsername());
        return Optional.empty();
    }
}

package gg.nya.imagehosting.services;

import gg.nya.imagehosting.models.Role;
import gg.nya.imagehosting.models.User;
import gg.nya.imagehosting.security.CustomAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuthenticationService {
    private final UserService userService;
    private final RoleService roleService;
    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    public AuthenticationService(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    public Optional<User> authenticate(String username, String password) {
        log.info("authenticate, authentication attempt for user {}", username);
        var user = userService.getUserByUsername(username.toLowerCase());
        if (user.isEmpty()) {
            log.debug("authenticate, user {} not found", username);
            return Optional.empty();
        }

        if (!comparePassword(password, user.get().getPassword())) {
            log.debug("authenticate, password mismatch for user {}", username);
            return Optional.empty();
        }

        List<String> userRoles = user.get().getRoles().stream().map(
                Role::getRole
        ).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new CustomAuthenticationToken(user.get().getId(), username, userRoles));

        log.info("authenticate, user {} authenticated", username);
        return user;
    }

    public void logout() {
        CustomAuthenticationToken authentication = (CustomAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null)
        {
            log.debug("logout, called without active authentication.");
            return;
        }
        log.info("logout, logging out user {}", authentication.getName());
        SecurityContextHolder.clearContext();
    }

    public Optional<User> signUp(String username, String password) {
        log.info("signUp, registration attempt for user {}", username);
        if (userService.getUserByUsername(username.toLowerCase()).isPresent()) {
            log.debug("signUp, user {} already exists", username);
            return Optional.empty();
        }

        var user = new User(username, hashPassword(password));
        userService.createUser(user);

        log.info("signUp, user {} registered", username);
        return Optional.of(user);
    }

    public boolean isCurrentUserAuthenticated() {
        return (SecurityContextHolder.getContext().getAuthentication() instanceof CustomAuthenticationToken);
    }

    public String getCurrentUsername() {
        CustomAuthenticationToken authentication = (CustomAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    public Long getCurrentUserId() {
        CustomAuthenticationToken authentication = (CustomAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        return authentication.getUserId();
    }

    public List<String> getCurrentUserRoles() {
        CustomAuthenticationToken authentication = (CustomAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        return authentication.getRoles();
    }

    private String hashPassword(String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.encode(password);
    }

    private boolean comparePassword(String password, String hashedPassword) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.matches(password, hashedPassword);
    }
}

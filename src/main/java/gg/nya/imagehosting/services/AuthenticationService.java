package gg.nya.imagehosting.services;

import gg.nya.imagehosting.models.Role;
import gg.nya.imagehosting.models.User;
import gg.nya.imagehosting.security.CustomAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuthenticationService {
    private final UserService userService;
    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    public AuthenticationService(UserService userService) {
        this.userService = userService;
    }

    public Optional<User> authenticate(String username, String password) {
        log.info("authenticate, authentication attempt for user {}", username);
        var user = userService.getUserByUsername(username.toLowerCase());
        if (user.isEmpty()) {
            log.debug("authenticate, user {} not found", username);
            return Optional.empty();
        }

        if (isRateLimitExceeded(user.get())) {
            log.debug("authenticate, user {} is rate limited", username);
            return Optional.empty();
        }

        if (!comparePassword(password, user.get().getPassword())) {
            log.debug("authenticate, password mismatch for user {}", username);
            userService.increaseFailedLoginAttempts(user.get());
            return Optional.empty();
        }

        List<String> userRoles = user.get().getRoles().stream().map(
                Role::getRole
        ).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new CustomAuthenticationToken(user.get().getId(), username, userRoles));

        userService.clearFailedLoginAttempts(user.get());
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

    private boolean isRateLimitExceeded(User user) {
        log.debug("isRateLimitExceeded, checking rate limit for user {} => {} failed attempts", user.getUsername(),
                user.getFailedLoginAttempts());
        // After 3 failed login attempts, lock the account for 15 minutes
        // After 5 failed login attempts, lock the account for 1 hour
        // After 10 failed login attempts, lock the account for 24 hours
        if(user.getFailedLoginAttempts() >= 3 && user.getFailedLoginAttempts() < 5) {
            return user.getLastLoginAttempt().plusMinutes(15).isAfter(LocalDateTime.now());
        } else if(user.getFailedLoginAttempts() >= 5 && user.getFailedLoginAttempts() < 10) {
            return user.getLastLoginAttempt().plusHours(1).isAfter(LocalDateTime.now());
        } else if(user.getFailedLoginAttempts() >= 10) {
            return user.getLastLoginAttempt().plusDays(1).isAfter(LocalDateTime.now());
        }
        return false;
    }
}

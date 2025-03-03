package gg.nya.imagehosting.services;

import gg.nya.imagehosting.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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

        if (!comparePassword(password, user.get().getPassword())) {
            log.debug("authenticate, password mismatch for user {}", username);
            return Optional.empty();
        }

        log.info("authenticate, user {} authenticated", username);
        return user;
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

    private String hashPassword(String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.encode(password);
    }

    private boolean comparePassword(String password, String hashedPassword) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.matches(password, hashedPassword);
    }
}

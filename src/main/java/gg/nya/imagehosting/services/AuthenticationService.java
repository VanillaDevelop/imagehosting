package gg.nya.imagehosting.services;

import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import gg.nya.imagehosting.models.User;

@Service
public class AuthenticationService {
    private final UserService userService;

    public AuthenticationService(UserService userService) {
        this.userService = userService;
    }

    public Optional<User> authenticate(String username, String password) {
        var user = userService.getUserByUsername(username.toLowerCase());
        if (user.isEmpty()) {
            return Optional.empty();
        }

        if (!comparePassword(password, user.get().getPassword())) {
            return Optional.empty();
        }

        return user;
    }

    public Optional<User> signUp(String username, String password) {
        if (userService.getUserByUsername(username.toLowerCase()).isPresent()) {
            return Optional.empty();
        }

        var user = new User(username, hashPassword(password));
        userService.createUser(user);
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

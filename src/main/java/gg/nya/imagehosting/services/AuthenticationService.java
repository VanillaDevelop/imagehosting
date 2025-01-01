package gg.nya.imagehosting.services;

import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import gg.nya.imagehosting.models.User;
import gg.nya.imagehosting.repositories.UserRepository;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;

    public AuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> authenticate(String username, String password) {
        var user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            return Optional.empty();
        }

        if (!user.get().getPassword().equals(hashPassword(password))) {
            return Optional.empty();
        }

        return user;
    }

    public Optional<User> signUp(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            return Optional.empty();
        }

        var user = new User(username, hashPassword(password));
        userRepository.save(user);
        return Optional.of(user);
    }

    private String hashPassword(String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.encode(password);
    }
}

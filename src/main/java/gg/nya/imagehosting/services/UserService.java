package gg.nya.imagehosting.services;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import gg.nya.imagehosting.models.User;
import gg.nya.imagehosting.repositories.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
}

package ru.rkjrth.adboard.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rkjrth.adboard.dto.AdminUserRequest;
import ru.rkjrth.adboard.entity.User;
import ru.rkjrth.adboard.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public Optional<User> getById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User createByAdmin(AdminUserRequest req) {
        if (req.getUsername() == null || req.getName() == null
                || req.getEmail() == null || req.getPassword() == null) {
            throw new IllegalArgumentException("username, name, email и password обязательны");
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("username уже занят");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("email уже занят");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole() != null ? req.getRole() : User.Role.USER);
        return userRepository.save(user);
    }

    @Transactional
    public Optional<User> updateByAdmin(Long id, AdminUserRequest req) {
        return userRepository.findById(id).map(existing -> {
            if (req.getUsername() != null) {
                userRepository.findByUsername(req.getUsername())
                        .filter(u -> !u.getId().equals(id))
                        .ifPresent(u -> {
                            throw new IllegalArgumentException("username уже занят");
                        });
                existing.setUsername(req.getUsername());
            }
            if (req.getName() != null) {
                existing.setName(req.getName());
            }
            if (req.getEmail() != null) {
                userRepository.findByEmail(req.getEmail())
                        .filter(u -> !u.getId().equals(id))
                        .ifPresent(u -> {
                            throw new IllegalArgumentException("email уже занят");
                        });
                existing.setEmail(req.getEmail());
            }
            if (req.getPassword() != null && !req.getPassword().isBlank()) {
                existing.setPasswordHash(passwordEncoder.encode(req.getPassword()));
            }
            if (req.getRole() != null) {
                existing.setRole(req.getRole());
            }
            return userRepository.save(existing);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }
        userRepository.deleteById(id);
        return true;
    }
}

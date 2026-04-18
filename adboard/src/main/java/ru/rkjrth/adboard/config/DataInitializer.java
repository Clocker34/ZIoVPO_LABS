package ru.rkjrth.adboard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.rkjrth.adboard.entity.User;
import ru.rkjrth.adboard.repository.UserRepository;

/**
 * Первый администратор создаётся только из переменных окружения (не из кода и не из SQL-скриптов).
 */
@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${adboard.bootstrap.admin.username:}")
    private String bootstrapAdminUsername;

    @Value("${adboard.bootstrap.admin.password:}")
    private String bootstrapAdminPassword;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (bootstrapAdminUsername == null || bootstrapAdminUsername.isBlank()
                || bootstrapAdminPassword == null || bootstrapAdminPassword.isBlank()) {
            return;
        }
        String u = bootstrapAdminUsername.trim();
        if (userRepository.findByUsername(u).isPresent()) {
            return;
        }
        User admin = new User();
        admin.setUsername(u);
        admin.setName("Administrator");
        admin.setEmail(u + "@bootstrap.local");
        admin.setRole(User.Role.ADMIN);
        admin.setPasswordHash(passwordEncoder.encode(bootstrapAdminPassword));
        userRepository.save(admin);
    }
}

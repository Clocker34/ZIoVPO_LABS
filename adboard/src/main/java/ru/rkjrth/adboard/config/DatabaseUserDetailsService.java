package ru.rkjrth.adboard.config;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.rkjrth.adboard.repository.UserRepository;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(user -> {
                    String hash = user.getPasswordHash();
                    if (hash == null || hash.isBlank()) {
                        throw new UsernameNotFoundException("User has no password set: " + username);
                    }
                    return org.springframework.security.core.userdetails.User
                            .withUsername(user.getUsername())
                            .password(hash)
                            .roles(user.getRole().name())
                            .build();
                })
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}

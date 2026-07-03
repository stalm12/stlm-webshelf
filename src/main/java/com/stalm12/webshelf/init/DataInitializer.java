package com.stalm12.webshelf.init;

import com.stalm12.webshelf.data.entity.AppUser;
import com.stalm12.webshelf.data.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("demo").isEmpty()) {
            AppUser demo = new AppUser();
            demo.setUsername("demo");
            demo.setPasswordHash(passwordEncoder.encode("demo123"));
            userRepository.save(demo);
        }
    }
}

package com.stalm12.webshelf.data.service;

import com.stalm12.webshelf.data.entity.AppUser;
import com.stalm12.webshelf.data.entity.Secret;
import com.stalm12.webshelf.data.repository.SecretRepository;
import com.stalm12.webshelf.data.repository.UserRepository;
import com.stalm12.webshelf.security.EncryptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class SecretService {

    private final SecretRepository secretRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    public SecretService(
            SecretRepository secretRepository,
            UserRepository userRepository,
            EncryptionService encryptionService
    ) {
        this.secretRepository = secretRepository;
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional(readOnly = true)
    public List<Secret> getSecretsForUser(String username) {
        return userRepository.findByUsername(username)
                .map(secretRepository::findByUserOrderByCreatedAtDesc)
                .orElse(Collections.emptyList());
    }

    public Secret addSecret(String username, String title, String plainValue, String description) throws Exception {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        String cleanTitle = title == null ? "" : title.trim();
        String cleanValue = plainValue == null ? "" : plainValue;
        if (cleanTitle.isEmpty() || cleanValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Title and value are required");
        }

        String encryptedValue = encryptionService.encrypt(cleanValue);

        Secret secret = new Secret();
        secret.setTitle(cleanTitle);
        secret.setEncryptedValue(encryptedValue);
        secret.setDescription(description);
        secret.setUser(user);
        secret.setCreatedAt(LocalDateTime.now());
        return secretRepository.save(secret);
    }

    @Transactional(readOnly = true)
    public String getDecryptedValue(Long secretId, String username) throws Exception {
        Secret secret = secretRepository.findById(secretId)
                .orElseThrow(() -> new IllegalArgumentException("Secret not found: " + secretId));
        if (!secret.getUser().getUsername().equals(username)) {
            throw new SecurityException("Not authorized to access this secret");
        }
        return encryptionService.decrypt(secret.getEncryptedValue());
    }

    public void deleteSecret(Long id, String username) {
        Secret secret = secretRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Secret not found: " + id));
        if (!secret.getUser().getUsername().equals(username)) {
            throw new SecurityException("Not authorized to delete this secret");
        }
        secretRepository.deleteById(id);
    }
}

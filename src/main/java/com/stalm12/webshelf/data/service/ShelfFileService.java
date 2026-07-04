package com.stalm12.webshelf.data.service;

import com.stalm12.webshelf.data.entity.AppUser;
import com.stalm12.webshelf.data.entity.ShelfFile;
import com.stalm12.webshelf.data.repository.ShelfFileRepository;
import com.stalm12.webshelf.data.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ShelfFileService {

    private final ShelfFileRepository shelfFileRepository;
    private final UserRepository userRepository;
    private final Path storageDirectory;

    public ShelfFileService(
            ShelfFileRepository shelfFileRepository,
            UserRepository userRepository,
            @Value("${app.file-storage-path:uploads}") String storageDirectory
    ) {
        this.shelfFileRepository = shelfFileRepository;
        this.userRepository = userRepository;
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<ShelfFile> getFilesForUser(String username) {
        return userRepository.findByUsername(username)
                .map(shelfFileRepository::findByUserOrderByCreatedAtDesc)
                .orElse(Collections.emptyList());
    }

    public ShelfFile storeFile(String username, String originalFilename, InputStream inputStream, long fileSize) throws IOException {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        String cleanFilename = sanitizeFilename(originalFilename);
        if (cleanFilename.isBlank()) {
            throw new IllegalArgumentException("Filename is required");
        }

        String publicId = UUID.randomUUID().toString();
        String storedFilename = publicId + "_" + cleanFilename;
        Path storedPath = resolveStoredPath(storedFilename);

        Files.createDirectories(storageDirectory);
        Files.copy(inputStream, storedPath, StandardCopyOption.REPLACE_EXISTING);

        ShelfFile shelfFile = new ShelfFile();
        shelfFile.setPublicId(publicId);
        shelfFile.setOriginalFilename(cleanFilename);
        shelfFile.setStoredFilename(storedFilename);
        shelfFile.setDownloadLink("/files/" + publicId + "/download");
        shelfFile.setSizeBytes(Math.max(fileSize, 0));
        shelfFile.setUser(user);
        shelfFile.setCreatedAt(LocalDateTime.now());

        return shelfFileRepository.save(shelfFile);
    }

    @Transactional(readOnly = true)
    public ShelfFile getOwnedFile(String publicId, String username) {
        return shelfFileRepository.findByPublicIdAndUserUsername(publicId, username)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + publicId));
    }

    public void deleteFile(String publicId, String username) {
        ShelfFile shelfFile = getOwnedFile(publicId, username);
        Path storedPath = resolveStoredPath(shelfFile.getStoredFilename());
        try {
            Files.deleteIfExists(storedPath);
        } catch (IOException e) {
            throw new IllegalStateException("Could not delete file from storage: " + publicId, e);
        }
        shelfFileRepository.delete(shelfFile);
    }

    public Path resolveStoredPath(String storedFilename) {
        return resolvePath(sanitizeFilename(storedFilename));
    }

    public Path resolveStoredPath(ShelfFile shelfFile) {
        return resolveStoredPath(shelfFile.getStoredFilename());
    }

    private Path resolvePath(String filename) {
        Path resolved = storageDirectory.resolve(filename).normalize();
        if (!resolved.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("Invalid file path");
        }
        return resolved;
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "";
        }
        String onlyFilename = Path.of(filename).getFileName().toString();
        return onlyFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

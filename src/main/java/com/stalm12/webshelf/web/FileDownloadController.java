package com.stalm12.webshelf.web;

import com.stalm12.webshelf.data.entity.ShelfFile;
import com.stalm12.webshelf.data.service.ShelfFileService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
public class FileDownloadController {

    private final ShelfFileService shelfFileService;

    public FileDownloadController(ShelfFileService shelfFileService) {
        this.shelfFileService = shelfFileService;
    }

    @GetMapping("/files/{publicId}/download")
    public ResponseEntity<Resource> download(@PathVariable String publicId, Authentication authentication) throws IOException {
        if (authentication == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Login required");
        }

        ShelfFile shelfFile;
        try {
            shelfFile = shelfFileService.getOwnedFile(publicId, authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(NOT_FOUND, "File not found");
        }
        Path filePath = shelfFileService.resolveStoredPath(shelfFile);
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists()) {
            throw new ResponseStatusException(NOT_FOUND, "File content not found");
        }

        String contentType = Files.probeContentType(filePath);
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(shelfFile.getOriginalFilename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(shelfFile.getSizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }
}

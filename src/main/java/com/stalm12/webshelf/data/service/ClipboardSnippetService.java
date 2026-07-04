package com.stalm12.webshelf.data.service;

import com.stalm12.webshelf.data.entity.AppUser;
import com.stalm12.webshelf.data.entity.ClipboardSnippet;
import com.stalm12.webshelf.data.repository.ClipboardSnippetRepository;
import com.stalm12.webshelf.data.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class ClipboardSnippetService {

    private final ClipboardSnippetRepository clipboardSnippetRepository;
    private final UserRepository userRepository;

    public ClipboardSnippetService(
            ClipboardSnippetRepository clipboardSnippetRepository,
            UserRepository userRepository
    ) {
        this.clipboardSnippetRepository = clipboardSnippetRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ClipboardSnippet> getSnippetsForUser(String username) {
        return userRepository.findByUsername(username)
                .map(clipboardSnippetRepository::findByUserOrderByCreatedAtDesc)
                .orElse(Collections.emptyList());
    }

    public ClipboardSnippet addSnippet(String username, String title, String content) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        String cleanTitle = title == null ? "" : title.trim();
        String rawContent = content == null ? "" : content;
        if (cleanTitle.isEmpty() || rawContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Title and content are required");
        }

        ClipboardSnippet snippet = new ClipboardSnippet();
        snippet.setTitle(cleanTitle);
        snippet.setContent(rawContent);
        snippet.setUser(user);
        snippet.setCreatedAt(LocalDateTime.now());
        return clipboardSnippetRepository.save(snippet);
    }

    public void deleteSnippet(Long id, String username) {
        ClipboardSnippet snippet = clipboardSnippetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Snippet not found: " + id));
        if (!snippet.getUser().getUsername().equals(username)) {
            throw new SecurityException("Not authorized to delete this snippet");
        }
        clipboardSnippetRepository.deleteById(id);
    }
}

package com.stalm12.webshelf.data.service;

import com.stalm12.webshelf.data.entity.AppUser;
import com.stalm12.webshelf.data.entity.WebLink;
import com.stalm12.webshelf.data.repository.UserRepository;
import com.stalm12.webshelf.data.repository.WebLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class WebLinkService {

    private final WebLinkRepository webLinkRepository;
    private final UserRepository userRepository;

    public WebLinkService(WebLinkRepository webLinkRepository, UserRepository userRepository) {
        this.webLinkRepository = webLinkRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<WebLink> getLinksForUser(String username) {
        return userRepository.findByUsername(username)
                .map(webLinkRepository::findByUserOrderByCreatedAtDesc)
                .orElse(Collections.emptyList());
    }

    public WebLink addLink(String username, String title, String url) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        WebLink link = new WebLink();
        link.setTitle(title);
        link.setUrl(url);
        link.setUser(user);
        link.setCreatedAt(LocalDateTime.now());

        return webLinkRepository.save(link);
    }

    public void deleteLink(Long id, String username) {
        WebLink link = webLinkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Link not found: " + id));
        if (!link.getUser().getUsername().equals(username)) {
            throw new SecurityException("Not authorized to delete this link");
        }
        webLinkRepository.deleteById(id);
    }
}

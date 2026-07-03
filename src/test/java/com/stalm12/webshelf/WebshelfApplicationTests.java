package com.stalm12.webshelf;

import com.stalm12.webshelf.data.entity.AppUser;
import com.stalm12.webshelf.data.entity.WebLink;
import com.stalm12.webshelf.data.repository.UserRepository;
import com.stalm12.webshelf.data.service.WebLinkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class WebshelfApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebLinkService webLinkService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void contextLoads() {
    }

    @Test
    void canSaveUserAndAddLinks() {
        AppUser user = new AppUser();
        user.setUsername("testuser");
        user.setPasswordHash(passwordEncoder.encode("secret"));
        userRepository.save(user);

        webLinkService.addLink("testuser", "Example", "https://example.com");
        webLinkService.addLink("testuser", "GitHub", "https://github.com");

        List<WebLink> links = webLinkService.getLinksForUser("testuser");
        assertThat(links).hasSize(2);
        assertThat(links).extracting(WebLink::getTitle).containsExactlyInAnyOrder("Example", "GitHub");
    }
}

package com.stalm12.webshelf;

import com.stalm12.webshelf.data.entity.AppUser;
import com.stalm12.webshelf.data.entity.ClipboardSnippet;
import com.stalm12.webshelf.data.entity.Secret;
import com.stalm12.webshelf.data.entity.ShelfFile;
import com.stalm12.webshelf.data.entity.WebLink;
import com.stalm12.webshelf.data.repository.UserRepository;
import com.stalm12.webshelf.data.service.ClipboardSnippetService;
import com.stalm12.webshelf.data.service.SecretService;
import com.stalm12.webshelf.data.service.ShelfFileService;
import com.stalm12.webshelf.data.service.WebLinkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class WebshelfApplicationTests {

    private static final Path TEST_UPLOAD_DIR;

    static {
        try {
            TEST_UPLOAD_DIR = Files.createTempDirectory("webshelf-tests-uploads");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.file-storage-path", TEST_UPLOAD_DIR::toString);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebLinkService webLinkService;

    @Autowired
    private ClipboardSnippetService clipboardSnippetService;

    @Autowired
    private SecretService secretService;

    @Autowired
    private ShelfFileService shelfFileService;

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

    @Test
    void canDeleteLink() {
        AppUser user = new AppUser();
        user.setUsername("deleteuser");
        user.setPasswordHash(passwordEncoder.encode("secret"));
        userRepository.save(user);

        WebLink link = webLinkService.addLink("deleteuser", "ToDelete", "https://delete.me");
        assertThat(webLinkService.getLinksForUser("deleteuser")).hasSize(1);

        webLinkService.deleteLink(link.getId(), "deleteuser");
        assertThat(webLinkService.getLinksForUser("deleteuser")).isEmpty();
    }

    @Test
    void deleteLinkByOtherUserIsRejected() {
        AppUser owner = new AppUser();
        owner.setUsername("owner");
        owner.setPasswordHash(passwordEncoder.encode("secret"));
        userRepository.save(owner);

        AppUser other = new AppUser();
        other.setUsername("other");
        other.setPasswordHash(passwordEncoder.encode("secret"));
        userRepository.save(other);

        WebLink link = webLinkService.addLink("owner", "Private", "https://private.com");

        org.junit.jupiter.api.Assertions.assertThrows(
                SecurityException.class,
                () -> webLinkService.deleteLink(link.getId(), "other")
        );
        assertThat(webLinkService.getLinksForUser("owner")).hasSize(1);
    }

    @Test
    void canStoreListAndDeleteFiles() throws IOException {
        AppUser user = new AppUser();
        user.setUsername("fileuser");
        user.setPasswordHash(passwordEncoder.encode("secret"));
        userRepository.save(user);

        byte[] content = "hello webshelf".getBytes(StandardCharsets.UTF_8);
        ShelfFile savedFile = shelfFileService.storeFile(
                "fileuser",
                "notes.txt",
                new ByteArrayInputStream(content),
                content.length
        );

        List<ShelfFile> files = shelfFileService.getFilesForUser("fileuser");
        assertThat(files).hasSize(1);
        assertThat(files.getFirst().getDownloadLink()).isEqualTo("/files/" + savedFile.getPublicId() + "/download");

        Path storedPath = shelfFileService.resolveStoredPath(savedFile);
        assertThat(Files.exists(storedPath)).isTrue();

        shelfFileService.deleteFile(savedFile.getPublicId(), "fileuser");
        assertThat(shelfFileService.getFilesForUser("fileuser")).isEmpty();
        assertThat(Files.exists(storedPath)).isFalse();
    }

    @Test
    void canSaveCopyAndDeleteClipboardSnippets() {
        AppUser user = new AppUser();
        user.setUsername("snippetuser");
        user.setPasswordHash(passwordEncoder.encode("secret"));
        userRepository.save(user);

        ClipboardSnippet snippet = clipboardSnippetService.addSnippet(
                "snippetuser",
                "Install script",
                "curl -fsSL https://example.test/install.sh | bash"
        );

        List<ClipboardSnippet> snippets = clipboardSnippetService.getSnippetsForUser("snippetuser");
        assertThat(snippets).hasSize(1);
        assertThat(snippets.getFirst().getContent()).contains("install.sh");

        clipboardSnippetService.deleteSnippet(snippet.getId(), "snippetuser");
        assertThat(clipboardSnippetService.getSnippetsForUser("snippetuser")).isEmpty();
    }

    @Test
    void deleteSnippetByOtherUserIsRejected() {
        AppUser owner = new AppUser();
        owner.setUsername("snippetowner");
        owner.setPasswordHash(passwordEncoder.encode("secret"));
        userRepository.save(owner);

        AppUser other = new AppUser();
        other.setUsername("snippetother");
        other.setPasswordHash(passwordEncoder.encode("secret"));
        userRepository.save(other);

        ClipboardSnippet snippet = clipboardSnippetService.addSnippet("snippetowner", "Token", "abc123");

        org.junit.jupiter.api.Assertions.assertThrows(
                SecurityException.class,
                () -> clipboardSnippetService.deleteSnippet(snippet.getId(), "snippetother")
        );
        assertThat(clipboardSnippetService.getSnippetsForUser("snippetowner")).hasSize(1);
    }

    @Test
    void canSaveEncryptAndDecryptSecrets() throws Exception {
        AppUser user = new AppUser();
        user.setUsername("secretuser");
        user.setPasswordHash(passwordEncoder.encode("secret"));
        userRepository.save(user);

        String plainValue = "sk_test_abc123xyz789";
        Secret savedSecret = secretService.addSecret(
                "secretuser",
                "Stripe API Key",
                plainValue,
                "Production key"
        );

        List<Secret> secrets = secretService.getSecretsForUser("secretuser");
        assertThat(secrets).hasSize(1);
        assertThat(secrets.getFirst().getEncryptedValue()).isNotEqualTo(plainValue);
        
        String decryptedValue = secretService.getDecryptedValue(savedSecret.getId(), "secretuser");
        assertThat(decryptedValue).isEqualTo(plainValue);

        secretService.deleteSecret(savedSecret.getId(), "secretuser");
        assertThat(secretService.getSecretsForUser("secretuser")).isEmpty();
    }

    @Test
    void deleteSecretByOtherUserIsRejected() throws Exception {
        AppUser owner = new AppUser();
        owner.setUsername("secretowner");
        owner.setPasswordHash(passwordEncoder.encode("secret"));
        userRepository.save(owner);

        AppUser other = new AppUser();
        other.setUsername("secretother");
        other.setPasswordHash(passwordEncoder.encode("secret"));
        userRepository.save(other);

        Secret secret = secretService.addSecret("secretowner", "Token", "abc123", null);

        org.junit.jupiter.api.Assertions.assertThrows(
                SecurityException.class,
                () -> secretService.deleteSecret(secret.getId(), "secretother")
        );
        assertThat(secretService.getSecretsForUser("secretowner")).hasSize(1);
    }

    @Test
    void cannotDecryptSecretAsOtherUser() throws Exception {
        AppUser owner = new AppUser();
        owner.setUsername("owner2");
        owner.setPasswordHash(passwordEncoder.encode("secret"));
        userRepository.save(owner);

        AppUser other = new AppUser();
        other.setUsername("other2");
        other.setPasswordHash(passwordEncoder.encode("secret"));
        userRepository.save(other);

        Secret secret = secretService.addSecret("owner2", "MySecret", "secret123", null);

        org.junit.jupiter.api.Assertions.assertThrows(
                SecurityException.class,
                () -> secretService.getDecryptedValue(secret.getId(), "other2")
        );
    }
}

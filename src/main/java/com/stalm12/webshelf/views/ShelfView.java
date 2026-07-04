package com.stalm12.webshelf.views;

import com.stalm12.webshelf.data.entity.ClipboardSnippet;
import com.stalm12.webshelf.data.entity.Secret;
import com.stalm12.webshelf.data.entity.ShelfFile;
import com.stalm12.webshelf.data.entity.WebLink;
import com.stalm12.webshelf.data.service.ClipboardSnippetService;
import com.stalm12.webshelf.data.service.SecretService;
import com.stalm12.webshelf.data.service.ShelfFileService;
import com.stalm12.webshelf.data.service.WebLinkService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Route("")
@PageTitle("My Webshelf")
@PermitAll
public class ShelfView extends VerticalLayout {

    private static final int MAX_UPLOAD_SIZE_BYTES = 5 * 1024 * 1024;

    private final WebLinkService webLinkService;
    private final ClipboardSnippetService clipboardSnippetService;
    private final SecretService secretService;
    private final ShelfFileService shelfFileService;
    private final Div linksContainer = new Div();
    private final Div snippetsContainer = new Div();
    private final Div secretsContainer = new Div();
    private final Div filesContainer = new Div();

    public ShelfView(
            WebLinkService webLinkService,
            ClipboardSnippetService clipboardSnippetService,
            SecretService secretService,
            ShelfFileService shelfFileService
    ) {
        this.webLinkService = webLinkService;
        this.clipboardSnippetService = clipboardSnippetService;
        this.secretService = secretService;
        this.shelfFileService = shelfFileService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        add(buildHeader(), buildContent());

        refreshLinks();
        refreshSnippets();
        refreshSecrets();
        refreshFiles();
    }

    private Component buildHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setPadding(true);
        header.getStyle()
                .set("background", "var(--lumo-primary-color)")
                .set("padding", "var(--lumo-space-m) var(--lumo-space-l)");

        H2 logo = new H2("Webshelf");
        logo.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-contrast-color)");

        String username = getCurrentUsername();
        Span userLabel = new Span("👤 " + username);
        userLabel.getStyle()
                .set("color", "var(--lumo-primary-contrast-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        Button logoutBtn = new Button("Logout");
        logoutBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        logoutBtn.getStyle().set("color", "var(--lumo-primary-contrast-color)");
        logoutBtn.addClickListener(e ->
                UI.getCurrent().getPage().setLocation("/logout"));

        header.add(logo, userLabel, logoutBtn);
        header.expand(logo);

        return header;
    }

    private Component buildContent() {
        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();
        content.setPadding(true);
        content.setSpacing(true);
        content.getStyle().set("max-width", "1200px").set("margin", "0 auto");

        content.add(buildLinksSection(), buildSnippetsSection(), buildSecretsSection(), buildFilesSection());

        return content;
    }

    private Component buildSnippetsSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.setWidthFull();

        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);

        H3 sectionTitle = new H3("My Clipboard");
        sectionTitle.getStyle().set("margin", "0");

        Button addBtn = new Button("+ Add Snippet", e -> openAddSnippetDialog());
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        toolbar.add(sectionTitle, addBtn);
        toolbar.expand(sectionTitle);

        snippetsContainer.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(320px, 1fr))")
                .set("gap", "var(--lumo-space-m)")
                .set("width", "100%")
                .set("padding-top", "var(--lumo-space-m)");

        section.add(toolbar, snippetsContainer);
        return section;
    }

    private Component buildSecretsSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.setWidthFull();

        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);

        H3 sectionTitle = new H3("My Secrets");
        sectionTitle.getStyle().set("margin", "0");

        Button addBtn = new Button("+ Add Secret", e -> openAddSecretDialog());
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        toolbar.add(sectionTitle, addBtn);
        toolbar.expand(sectionTitle);

        secretsContainer.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(320px, 1fr))")
                .set("gap", "var(--lumo-space-m)")
                .set("width", "100%")
                .set("padding-top", "var(--lumo-space-m)");

        section.add(toolbar, secretsContainer);
        return section;
    }

    private Component buildLinksSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.setWidthFull();

        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);

        H3 sectionTitle = new H3("My Links");
        sectionTitle.getStyle().set("margin", "0");

        Button addBtn = new Button("+ Add Link", e -> openAddDialog());
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        toolbar.add(sectionTitle, addBtn);
        toolbar.expand(sectionTitle);

        linksContainer.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(300px, 1fr))")
                .set("gap", "var(--lumo-space-m)")
                .set("width", "100%")
                .set("padding-top", "var(--lumo-space-m)");

        section.add(toolbar, linksContainer);
        return section;
    }

    private Component buildFilesSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.setWidthFull();

        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);

        H3 sectionTitle = new H3("My Files");
        sectionTitle.getStyle().set("margin", "0");

        Upload upload = buildFileUpload();

        toolbar.add(sectionTitle, upload);
        toolbar.expand(sectionTitle);

        filesContainer.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(320px, 1fr))")
                .set("gap", "var(--lumo-space-m)")
                .set("width", "100%")
                .set("padding-top", "var(--lumo-space-m)");

        section.add(toolbar, filesContainer);
        return section;
    }

    private Upload buildFileUpload() {
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setMaxFiles(1);
        upload.setMaxFileSize(MAX_UPLOAD_SIZE_BYTES);
        upload.setDropAllowed(true);
        upload.setUploadButton(new Button("Upload file"));
        upload.addSucceededListener(event -> {
            try (InputStream input = buffer.getInputStream()) {
                shelfFileService.storeFile(
                        getCurrentUsername(),
                        event.getFileName(),
                        input,
                        event.getContentLength()
                );
                refreshFiles();
                Notification n = Notification.show("File uploaded.", 2000, Notification.Position.BOTTOM_END);
                n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IllegalArgumentException ex) {
                Notification n = Notification.show("Upload failed: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (IOException ex) {
                Notification n = Notification.show("Upload failed: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        upload.addFileRejectedListener(event -> {
            Notification n = Notification.show(event.getErrorMessage(), 3000, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        });
        return upload;
    }

    private void openAddDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add New Link");
        dialog.setWidth("400px");

        TextField titleField = new TextField("Title");
        titleField.setWidthFull();
        titleField.setPlaceholder("e.g. GitHub");
        titleField.setAutofocus(true);

        TextField urlField = new TextField("URL");
        urlField.setWidthFull();
        urlField.setPlaceholder("https://github.com");

        VerticalLayout fields = new VerticalLayout(titleField, urlField);
        fields.setPadding(false);
        fields.setSpacing(true);
        dialog.add(fields);

        Button saveBtn = new Button("Add", e -> {
            String title = titleField.getValue().trim();
            String url = urlField.getValue().trim();

            if (title.isEmpty() || url.isEmpty()) {
                Notification.show("Title and URL are required.", 3000, Notification.Position.MIDDLE);
                return;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            webLinkService.addLink(getCurrentUsername(), title, url);
            dialog.close();
            refreshLinks();

            Notification n = Notification.show("Link added!", 2000, Notification.Position.BOTTOM_END);
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);

        dialog.open();
    }

    private void openAddSnippetDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Clipboard Snippet");
        dialog.setWidth("600px");

        TextField titleField = new TextField("Title");
        titleField.setWidthFull();
        titleField.setPlaceholder("e.g. API token, setup script");
        titleField.setAutofocus(true);

        TextArea contentField = new TextArea("Snippet");
        contentField.setWidthFull();
        contentField.setMinHeight("220px");
        contentField.setPlaceholder("Paste your text/code snippet here");

        VerticalLayout fields = new VerticalLayout(titleField, contentField);
        fields.setPadding(false);
        fields.setSpacing(true);
        dialog.add(fields);

        Button saveBtn = new Button("Save", e -> {
            String title = titleField.getValue();
            String content = contentField.getValue();
            try {
                clipboardSnippetService.addSnippet(getCurrentUsername(), title, content);
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
                return;
            }

            dialog.close();
            refreshSnippets();
            Notification n = Notification.show("Snippet saved.", 2000, Notification.Position.BOTTOM_END);
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void refreshLinks() {
        linksContainer.removeAll();
        List<WebLink> links = webLinkService.getLinksForUser(getCurrentUsername());

        if (links.isEmpty()) {
            Div emptyState = new Div();
            emptyState.getStyle()
                    .set("text-align", "center")
                    .set("padding", "var(--lumo-space-xl)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("grid-column", "1 / -1");
            emptyState.add(new Paragraph("No links yet."),
                    new Paragraph("Click \"+ Add Link\" to save your first web link."));
            linksContainer.add(emptyState);
        } else {
            links.forEach(link -> linksContainer.add(buildLinkCard(link)));
        }
    }

    private void refreshFiles() {
        filesContainer.removeAll();
        List<ShelfFile> files = shelfFileService.getFilesForUser(getCurrentUsername());

        if (files.isEmpty()) {
            Div emptyState = new Div();
            emptyState.getStyle()
                    .set("text-align", "center")
                    .set("padding", "var(--lumo-space-xl)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("grid-column", "1 / -1");
            emptyState.add(new Paragraph("No files yet."),
                    new Paragraph("Upload a small file to access it later from any device."));
            filesContainer.add(emptyState);
        } else {
            files.forEach(file -> filesContainer.add(buildFileCard(file)));
        }
    }

    private void refreshSnippets() {
        snippetsContainer.removeAll();
        List<ClipboardSnippet> snippets = clipboardSnippetService.getSnippetsForUser(getCurrentUsername());

        if (snippets.isEmpty()) {
            Div emptyState = new Div();
            emptyState.getStyle()
                    .set("text-align", "center")
                    .set("padding", "var(--lumo-space-xl)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("grid-column", "1 / -1");
            emptyState.add(new Paragraph("No snippets yet."),
                    new Paragraph("Click \"+ Add Snippet\" and copy content from anywhere."));
            snippetsContainer.add(emptyState);
        } else {
            snippets.forEach(snippet -> snippetsContainer.add(buildSnippetCard(snippet)));
        }
    }


    private Component buildLinkCard(WebLink link) {
        Div card = createCard();

        Span titleSpan = new Span("🔗 " + link.getTitle());
        titleSpan.getStyle()
                .set("font-weight", "600")
                .set("font-size", "var(--lumo-font-size-m)");

        Anchor urlAnchor = new Anchor(link.getUrl(), link.getUrl());
        urlAnchor.setTarget("_blank");
        urlAnchor.setTitle(link.getUrl());
        urlAnchor.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-primary-color)")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap")
                .set("display", "block")
                .set("max-width", "100%");

        Span dateSpan = new Span(link.getCreatedAt().toLocalDate().toString());
        dateSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-tertiary-text-color)");

        HorizontalLayout actions = createActionBar();

        Button openBtn = new Button("Open");
        openBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        openBtn.addClickListener(e -> UI.getCurrent().getPage().open(link.getUrl(), "_blank"));

        Button deleteBtn = new Button("Delete");
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        deleteBtn.addClickListener(e -> {
            Dialog confirm = new Dialog();
            confirm.setHeaderTitle("Delete Link");
            confirm.add(new Paragraph("Are you sure you want to delete \"" + link.getTitle() + "\"?"));
            Button yes = new Button("Delete", ev -> {
                webLinkService.deleteLink(link.getId(), getCurrentUsername());
                confirm.close();
                refreshLinks();
            });
            yes.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
            Button no = new Button("Cancel", ev -> confirm.close());
            confirm.getFooter().add(no, yes);
            confirm.open();
        });

        actions.add(openBtn, deleteBtn);
        card.add(titleSpan, urlAnchor, dateSpan, actions);
        return card;
    }

    private Component buildFileCard(ShelfFile shelfFile) {
        Div card = createCard();

        Span titleSpan = new Span("📄 " + shelfFile.getOriginalFilename());
        titleSpan.getStyle()
                .set("font-weight", "600")
                .set("font-size", "var(--lumo-font-size-m)");

        Anchor fileAnchor = new Anchor(shelfFile.getDownloadLink(), shelfFile.getDownloadLink());
        fileAnchor.setTitle(shelfFile.getDownloadLink());
        fileAnchor.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-primary-color)")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap")
                .set("display", "block")
                .set("max-width", "100%");

        Span metaSpan = new Span(
                humanReadableSize(shelfFile.getSizeBytes()) + " • " + shelfFile.getCreatedAt().toLocalDate()
        );
        metaSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-tertiary-text-color)");

        HorizontalLayout actions = createActionBar();

        Button downloadBtn = new Button("Download");
        downloadBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        downloadBtn.addClickListener(e -> UI.getCurrent().getPage().open(shelfFile.getDownloadLink(), "_blank"));

        Button deleteBtn = new Button("Delete");
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        deleteBtn.addClickListener(e -> {
            Dialog confirm = new Dialog();
            confirm.setHeaderTitle("Delete File");
            confirm.add(new Paragraph("Are you sure you want to delete \"" + shelfFile.getOriginalFilename() + "\"?"));
            Button yes = new Button("Delete", ev -> {
                shelfFileService.deleteFile(shelfFile.getPublicId(), getCurrentUsername());
                confirm.close();
                refreshFiles();
            });
            yes.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
            Button no = new Button("Cancel", ev -> confirm.close());
            confirm.getFooter().add(no, yes);
            confirm.open();
        });

        actions.add(downloadBtn, deleteBtn);
        card.add(titleSpan, fileAnchor, metaSpan, actions);
        return card;
    }

    private Component buildSnippetCard(ClipboardSnippet snippet) {
        Div card = createCard();

        Span titleSpan = new Span("📋 " + snippet.getTitle());
        titleSpan.getStyle()
                .set("font-weight", "600")
                .set("font-size", "var(--lumo-font-size-m)");

        Pre contentPre = new Pre(snippet.getContent());
        contentPre.getStyle()
                .set("margin", "0")
                .set("padding", "var(--lumo-space-s)")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("max-height", "180px")
                .set("overflow", "auto")
                .set("white-space", "pre-wrap")
                .set("word-break", "break-word");

        Span dateSpan = new Span(snippet.getCreatedAt().toLocalDate().toString());
        dateSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-tertiary-text-color)");

        HorizontalLayout actions = createActionBar();

        Button copyBtn = new Button("Copy");
        copyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        copyBtn.addClickListener(e -> {
            UI.getCurrent().getPage().executeJs("navigator.clipboard.writeText($0)", snippet.getContent());
            Notification n = Notification.show("Copied to clipboard.", 1500, Notification.Position.BOTTOM_END);
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

        Button deleteBtn = new Button("Delete");
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        deleteBtn.addClickListener(e -> {
            Dialog confirm = new Dialog();
            confirm.setHeaderTitle("Delete Snippet");
            confirm.add(new Paragraph("Are you sure you want to delete \"" + snippet.getTitle() + "\"?"));
            Button yes = new Button("Delete", ev -> {
                clipboardSnippetService.deleteSnippet(snippet.getId(), getCurrentUsername());
                confirm.close();
                refreshSnippets();
            });
            yes.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
            Button no = new Button("Cancel", ev -> confirm.close());
            confirm.getFooter().add(no, yes);
            confirm.open();
        });

        actions.add(copyBtn, deleteBtn);
        card.add(titleSpan, contentPre, dateSpan, actions);
        return card;
    }

    private void openAddSecretDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Secret/Credential");
        dialog.setWidth("600px");

        TextField titleField = new TextField("Title");
        titleField.setWidthFull();
        titleField.setPlaceholder("e.g. API Key, JWT Token");
        titleField.setAutofocus(true);

        TextArea valueField = new TextArea("Secret Value");
        valueField.setWidthFull();
        valueField.setMinHeight("120px");
        valueField.setPlaceholder("Paste your secret here (will be encrypted)");

        TextField descriptionField = new TextField("Description (optional)");
        descriptionField.setWidthFull();
        descriptionField.setPlaceholder("e.g. Production API key for Service X");

        VerticalLayout fields = new VerticalLayout(titleField, valueField, descriptionField);
        fields.setPadding(false);
        fields.setSpacing(true);
        dialog.add(fields);

        Button saveBtn = new Button("Save", e -> {
            String title = titleField.getValue();
            String value = valueField.getValue();
            String description = descriptionField.getValue();
            try {
                secretService.addSecret(getCurrentUsername(), title, value, description);
            } catch (Exception ex) {
                Notification n = Notification.show("Error: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            dialog.close();
            refreshSecrets();
            Notification n = Notification.show("Secret saved (encrypted).", 2000, Notification.Position.BOTTOM_END);
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void refreshSecrets() {
        secretsContainer.removeAll();
        List<Secret> secrets = secretService.getSecretsForUser(getCurrentUsername());

        if (secrets.isEmpty()) {
            Div emptyState = new Div();
            emptyState.getStyle()
                    .set("text-align", "center")
                    .set("padding", "var(--lumo-space-xl)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("grid-column", "1 / -1");
            emptyState.add(new Paragraph("No secrets yet."),
                    new Paragraph("Click \"+ Add Secret\" to store API keys, passwords, etc. (encrypted)."));
            secretsContainer.add(emptyState);
        } else {
            secrets.forEach(secret -> secretsContainer.add(buildSecretCard(secret)));
        }
    }

    private Component buildSecretCard(Secret secret) {
        Div card = createCard();

        Span titleSpan = new Span("🔐 " + secret.getTitle());
        titleSpan.getStyle()
                .set("font-weight", "600")
                .set("font-size", "var(--lumo-font-size-m)");

        Span descSpan = new Span();
        if (secret.getDescription() != null && !secret.getDescription().isBlank()) {
            descSpan.setText(secret.getDescription());
            descSpan.getStyle()
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("color", "var(--lumo-secondary-text-color)");
        }

        Span maskedSpan = new Span("••••••••");
        maskedSpan.getStyle()
                .set("font-family", "monospace")
                .set("padding", "var(--lumo-space-s)")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("display", "block")
                .set("margin-top", "var(--lumo-space-xs)");

        Span dateSpan = new Span(secret.getCreatedAt().toLocalDate().toString());
        dateSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-tertiary-text-color)");

        HorizontalLayout actions = createActionBar();

        Button copyBtn = new Button("Copy");
        copyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        copyBtn.addClickListener(e -> {
            try {
                String decryptedValue = secretService.getDecryptedValue(secret.getId(), getCurrentUsername());
                UI.getCurrent().getPage().executeJs("navigator.clipboard.writeText($0)", decryptedValue);
                Notification n = Notification.show("Secret copied to clipboard.", 1500, Notification.Position.BOTTOM_END);
                n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification n = Notification.show("Error: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button revealBtn = new Button("Reveal");
        revealBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        revealBtn.addClickListener(e -> {
            try {
                String decryptedValue = secretService.getDecryptedValue(secret.getId(), getCurrentUsername());
                Dialog revealDialog = new Dialog();
                revealDialog.setHeaderTitle("Secret Value - " + secret.getTitle());
                revealDialog.setWidth("500px");
                TextArea valueDisplay = new TextArea();
                valueDisplay.setValue(decryptedValue);
                valueDisplay.setReadOnly(true);
                valueDisplay.setWidthFull();
                valueDisplay.setMinHeight("150px");
                revealDialog.add(valueDisplay);
                Button closeBtn = new Button("Close", ev -> revealDialog.close());
                revealDialog.getFooter().add(closeBtn);
                revealDialog.open();
            } catch (Exception ex) {
                Notification n = Notification.show("Error: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button deleteBtn = new Button("Delete");
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        deleteBtn.addClickListener(e -> {
            Dialog confirm = new Dialog();
            confirm.setHeaderTitle("Delete Secret");
            confirm.add(new Paragraph("Are you sure you want to delete \"" + secret.getTitle() + "\"?"));
            Button yes = new Button("Delete", ev -> {
                secretService.deleteSecret(secret.getId(), getCurrentUsername());
                confirm.close();
                refreshSecrets();
            });
            yes.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
            Button no = new Button("Cancel", ev -> confirm.close());
            confirm.getFooter().add(no, yes);
            confirm.open();
        });

        actions.add(copyBtn, revealBtn, deleteBtn);
        card.add(titleSpan);
        if (secret.getDescription() != null && !secret.getDescription().isBlank()) {
            card.add(descSpan);
        }
        card.add(maskedSpan, dateSpan, actions);
        return card;
    }

    private Div createCard() {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "var(--lumo-space-xs)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("transition", "box-shadow 0.15s ease");
        return card;
    }

    private HorizontalLayout createActionBar() {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setJustifyContentMode(JustifyContentMode.END);
        actions.setWidthFull();
        actions.setSpacing(true);
        actions.getStyle().set("margin-top", "var(--lumo-space-s)");
        return actions;
    }

    private String humanReadableSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.ROOT, "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        return String.format(Locale.ROOT, "%.1f MB", mb);
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}

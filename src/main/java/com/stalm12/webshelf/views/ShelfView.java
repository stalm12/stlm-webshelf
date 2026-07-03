package com.stalm12.webshelf.views;

import com.stalm12.webshelf.data.entity.WebLink;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Route("")
@PageTitle("My Webshelf")
@PermitAll
public class ShelfView extends VerticalLayout {

    private final WebLinkService webLinkService;
    private final Div linksContainer = new Div();

    public ShelfView(WebLinkService webLinkService) {
        this.webLinkService = webLinkService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        add(buildHeader(), buildContent());

        refreshLinks();
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
        content.getStyle().set("max-width", "1200px").set("margin", "0 auto");

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

        content.add(toolbar, linksContainer);

        return content;
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

    private Component buildLinkCard(WebLink link) {
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

        HorizontalLayout actions = new HorizontalLayout();
        actions.setJustifyContentMode(JustifyContentMode.END);
        actions.setWidthFull();
        actions.setSpacing(true);
        actions.getStyle().set("margin-top", "var(--lumo-space-s)");

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

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}

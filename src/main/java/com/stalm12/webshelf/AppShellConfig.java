package com.stalm12.webshelf;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;

@PWA(name = "Webshelf", shortName = "Webshelf", offlineResources = {})
@Push
public class AppShellConfig implements AppShellConfigurator {
}

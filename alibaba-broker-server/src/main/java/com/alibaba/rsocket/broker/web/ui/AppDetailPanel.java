package com.alibaba.rsocket.broker.web.ui;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.broker.web.ui.component.Markdown;
import com.vaadin.flow.component.html.*;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.emoji.EmojiImageType;
import com.vladsch.flexmark.ext.emoji.EmojiShortcutType;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * App Detail Panel
 *
 * @author leijuan
 */
public class AppDetailPanel extends Div {
    private static Parser MARKDOWN_PARSER;
    private static HtmlRenderer MARKDOWN_RENDER;
    private H2 title = new H2("App Detail");
    H4 securityInfoHeader = new H4("Security Info");
    private Pre securityInfo = new Pre();
    private Paragraph description = new Paragraph();
    H4 metadataHeader = new H4("App Metadata");
    private Pre metadata = new Pre();
    H4 publishedServicesHeader = new H4("Published Services");
    private Pre publishedServices = new Pre();
    H4 consumedServicesHeader = new H4("Consumed Services");
    private Pre consumedServices = new Pre();
    H4 humansTextHeader = new H4("HumansText");
    private Markdown humans = new Markdown("");


    static {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));
        options.set(HtmlRenderer.SOFT_BREAK, "<br/>\n");
        options.set(Parser.EXTENSIONS, Collections.singleton(EmojiExtension.create()));
        options.set(EmojiExtension.USE_SHORTCUT_TYPE, EmojiShortcutType.EMOJI_CHEAT_SHEET);
        options.set(EmojiExtension.USE_IMAGE_TYPE, EmojiImageType.UNICODE_ONLY);
        MARKDOWN_PARSER = Parser.builder(options).build();
        MARKDOWN_RENDER = HtmlRenderer.builder(options).build();
    }

    public AppDetailPanel() {
        add(title);
        add(securityInfoHeader);
        add(securityInfo);
        add(metadataHeader);
        add(metadata);
        add(publishedServicesHeader);
        add(publishedServices);
        add(consumedServicesHeader);
        add(consumedServices);
        //add(description);
        add(humansTextHeader);
        add(humans);
        clear();
    }

    public void clear() {
        this.title.setVisible(false);
        this.securityInfoHeader.setVisible(false);
        this.securityInfo.setVisible(false);
        this.metadataHeader.setVisible(false);
        this.metadata.setVisible(false);
        this.publishedServicesHeader.setVisible(false);
        this.publishedServices.setVisible(false);
        this.consumedServicesHeader.setVisible(false);
        this.consumedServices.setVisible(false);
        this.humansTextHeader.setVisible(false);
        this.humans.setVisible(false);

    }

    public void setAppName(String appName) {
        this.title.setVisible(true);
        this.title.setText("App Detail: " + appName);
    }

    public void setMetadata(Map<String, String> appMetadata) {
        String metadataList = null;
        if (appMetadata != null) {
            metadataList = appMetadata.entrySet().stream()
                    .filter(entry -> !entry.getKey().startsWith("_"))
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .collect(Collectors.joining("\r\n"));
        }
        if (metadataList != null && !metadataList.isEmpty()) {
            this.metadataHeader.setVisible(true);
            this.metadata.setVisible(true);
            this.metadata.setText(metadataList);
        } else {
            this.metadataHeader.setVisible(false);
            this.metadata.setText("");
            this.metadata.setVisible(false);
        }
    }

    public void setSecurityInfo(String orgs, String serviceAccounts, String roles) {
        this.securityInfo.setVisible(true);
        this.securityInfoHeader.setVisible(true);
        this.securityInfo.setText("Orgs: " + orgs + "\r\n" + "ServiceAccounts: " + serviceAccounts + "\r\n" + "Roles: " + roles);
    }

    public void setPublishedServices(Set<ServiceLocator> publishedServices) {
        if (publishedServices != null && !publishedServices.isEmpty()) {
            this.publishedServicesHeader.setVisible(true);
            this.publishedServices.setVisible(true);
            this.publishedServices.setText(publishedServices.stream().map(ServiceLocator::getGsv).collect(Collectors.joining("\r\n")));
        } else {
            this.publishedServicesHeader.setVisible(false);
            this.publishedServices.setVisible(false);
            this.publishedServices.setText("");
        }
    }

    public void setConsumedServices(Set<String> consumedServices) {
        if (consumedServices != null && !consumedServices.isEmpty()) {
            this.consumedServicesHeader.setVisible(true);
            this.consumedServices.setVisible(true);
            this.consumedServices.setText(String.join("\r\n", consumedServices));
        } else {
            this.consumedServicesHeader.setVisible(false);
            this.consumedServices.setVisible(false);
            this.consumedServices.setText("");
        }
    }

    public void setDescription(String description) {
        //this.description.setText(description);
    }

    public void setHumans(String humans) {
        if (humans != null && !humans.isEmpty()) {
            this.humansTextHeader.setVisible(true);
            this.humans.setVisible(true);
            Node document = MARKDOWN_PARSER.parse(humans);
            String html = MARKDOWN_RENDER.render(document);
            this.humans.setText(html);
        } else {
            this.humansTextHeader.setVisible(false);
            this.humans.setVisible(false);
            this.humans.setText("");
        }
    }
}

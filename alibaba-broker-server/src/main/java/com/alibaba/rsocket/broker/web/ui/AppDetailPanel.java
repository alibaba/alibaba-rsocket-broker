package com.alibaba.rsocket.broker.web.ui;

import com.alibaba.rsocket.broker.web.ui.component.Markdown;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
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

/**
 * App Detail Panel
 *
 * @author leijuan
 */
public class AppDetailPanel extends Div {
    private static Parser MARKDOWN_PARSER;
    private static HtmlRenderer MARKDOWN_RENDER;
    private H3 title = new H3("App Detail");
    private Paragraph description = new Paragraph();
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
        add(description);
        add(humans);
    }

    public void setAppName(String appName) {
        this.title.setText("App Detail: " + appName);
    }

    public void setDescription(String description) {
        this.description.setText(description);
    }

    public void setHumans(String humans) {
        if (humans != null && !humans.isEmpty()) {
            Node document = MARKDOWN_PARSER.parse(humans);
            String html = MARKDOWN_RENDER.render(document);
            this.humans.setText(html);
        } else {
            this.humans.setText("");
        }
    }
}

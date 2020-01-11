package com.alibaba.rsocket.broker.web.ui;

import com.alibaba.rsocket.broker.web.ui.component.Markdown;
import com.vaadin.flow.component.html.*;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.Arrays;

/**
 * App Detail Panel
 *
 * @author leijuan
 */
public class AppDetailPanel extends Div {
    private H3 title = new H3("App Detail");
    private Paragraph description = new Paragraph();
    private Markdown humans = new Markdown("");
    private  Parser parser;
    private HtmlRenderer render;
    public AppDetailPanel() {
        add(title);
        add(description);
        add(humans);
        init();
    }
    private void  init(){
        MutableDataSet options = new MutableDataSet();

        options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));
        options.set(HtmlRenderer.SOFT_BREAK, "<br/>\n");

        this.parser = Parser.builder(options).build();
        this.render = HtmlRenderer.builder(options).build();
    }


    public void setAppName(String appName) {
        this.title.setText("App Detail: " + appName);
    }

    public void setDescription(String description) {
        this.description.setText(description);
    }

    public void setHumans(String humans) {
        Node document = parser.parse(humans);
        String html = render.render(document);
        this.humans.setText(html);
    }
}

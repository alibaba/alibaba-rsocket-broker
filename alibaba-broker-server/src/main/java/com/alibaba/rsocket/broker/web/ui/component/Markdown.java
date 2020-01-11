package com.alibaba.rsocket.broker.web.ui.component;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Span;

/**
 * A component to show HTML text.
 */
public class Markdown extends Composite<Span> implements HasText {

    private Span content = new Span();
    private String text;

    public Markdown(String htmlText) {
        setText(htmlText);
    }

    @Override
    protected Span initContent() {
        return content;
    }

    @Override
    public void setText(String htmlText) {
        if(htmlText == null) {
            htmlText = "";
        }
        if(htmlText.equals(text)) {
            return;
        }
        text = htmlText;
        content.removeAll();
        content.add(new Html("<article class=\"markdown-body\">" + htmlText + "</article>"));
    }

    @Override
    public String getText() {
        return text;
    }
}
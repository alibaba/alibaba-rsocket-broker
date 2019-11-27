package com.alibaba.rsocket.broker.web.ui;

import com.alibaba.spring.boot.rsocket.broker.services.ConfigurationService;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import static com.alibaba.rsocket.broker.web.ui.AppConfigView.NAV;

/**
 * App Config View, config settings
 *
 * @author leijuan
 */
@Route(value = NAV, layout = MainLayout.class)
public class AppConfigView extends VerticalLayout {
    public static final String NAV = "AppConfigView";
    private ConfigurationService configurationService;
    private TextField appName;
    private TextField configName;
    private TextArea configValue;
    private Button saveButton;

    public AppConfigView(@Autowired ConfigurationService configurationService) {
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        this.configurationService = configurationService;
        VerticalLayout appList = makeAppList();
        VerticalLayout content = makeConfigForm();
        // Compose layout
        horizontalLayout.add(appList, content);
        add(horizontalLayout);
    }

    VerticalLayout makeConfigForm() {
        VerticalLayout content = new VerticalLayout();
        appName = new TextField("App Name");
        appName.setWidth("300px");
        configName = new TextField("Key");
        configName.setWidth("300px");
        configValue = new TextArea("Value");
        configValue.setWidth("600px");
        HorizontalLayout buttons = new HorizontalLayout();
        saveButton = new Button("Save", buttonClickEvent -> {
            String key = appName.getValue() + ":" + configName.getValue();
            configurationService.put(key, configValue.getValue())
                    .doOnSuccess(aVoid -> Notification.show("Saved Successfully"))
                    .subscribe();
        });
        content.add(new H3("Key/Value"));
        content.add(appName);
        content.add(configName);
        content.add(configValue);
        buttons.add(saveButton);
        buttons.add(new Button("Clear", buttonClickEvent -> {
            clearForm();
        }));
        content.add(buttons);
        return content;
    }


    VerticalLayout makeAppList() {
        VerticalLayout appList = new VerticalLayout();
        appList.add(new H3("App List"));
        Accordion accordion = new Accordion();
        configurationService.getGroups().subscribe(groupName -> {
            UnorderedList keys = new UnorderedList();
            configurationService.findNamesByGroup(groupName).subscribe(key -> {
                keys.add(makeConfigItem(groupName, key.substring(key.indexOf(":") + 1)));
            });
            accordion.add(groupName, keys);
        });
        accordion.addOpenedChangeListener(openedChangeEvent -> {
            openedChangeEvent.getOpenedPanel().ifPresent(accordionPanel -> {
                clearForm();
                appName.setValue(accordionPanel.getSummaryText());
            });
        });
        appList.add(accordion);
        return appList;
    }

    ListItem makeConfigItem(String appName, String configName) {
        ListItem item = new ListItem(configName);
        item.getStyle().set("text-decoration", "underline").set("cursor", "pointer");
        item.addClickListener(listItemClickEvent -> {
            this.appName.setValue(appName);
            this.configName.setValue(configName);
            configurationService.get(appName + ":" + configName).subscribe(s -> {
                this.configValue.setValue(s);
            });
        });
        return item;
    }

    private void clearForm() {
        appName.clear();
        configName.clear();
        configValue.clear();
    }

}

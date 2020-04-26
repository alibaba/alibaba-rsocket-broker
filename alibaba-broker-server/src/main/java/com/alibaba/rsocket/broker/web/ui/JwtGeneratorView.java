package com.alibaba.rsocket.broker.web.ui;

import com.alibaba.spring.boot.rsocket.broker.security.AuthenticationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.util.UUID;

import static com.alibaba.rsocket.broker.web.ui.JwtGeneratorView.NAV;


/**
 * JWT generator view
 *
 * @author leijuan
 */
@Route(value = NAV, layout = MainLayout.class)
public class JwtGeneratorView extends VerticalLayout {
    public static final String NAV = "JwtGeneratorView";
    private TextField appNameText;
    private TextField ownersText;
    private TextField orgIdsText;
    private TextField serviceAccountsText;
    private TextField rolesText;
    private TextField authoritiesText;
    private TextArea tokenTextArea;
    private Button generateBtn;
    private AuthenticationService authenticationService;

    public JwtGeneratorView(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
        add(new H1("JWT Token Generator"));
        add(makeGeneratorForm());
    }

    VerticalLayout makeGeneratorForm() {
        VerticalLayout content = new VerticalLayout();
        appNameText = new TextField("App Name");
        appNameText.setWidth("300px");
        appNameText.setPlaceholder("your-app-name");
        ownersText = new TextField("Owners");
        ownersText.setWidth("300px");
        orgIdsText = new TextField("Org IDs");
        orgIdsText.setPlaceholder("1");
        orgIdsText.setWidth("300px");
        serviceAccountsText = new TextField("Service Accounts");
        serviceAccountsText.setValue("default");
        serviceAccountsText.setWidth("300px");
        rolesText = new TextField("Roles");
        rolesText.setWidth("300px");
        rolesText.setValue("user");
        authoritiesText = new TextField("Authorities");
        rolesText.setWidth("300px");
        rolesText.setValue("");
        tokenTextArea = new TextArea("JWT Token");
        tokenTextArea.setWidth("800px");
        tokenTextArea.setHeight("240px");
        tokenTextArea.setReadOnly(true);
        rolesText.setWidth("200px");
        HorizontalLayout buttons = new HorizontalLayout();
        generateBtn = new Button("Generate", buttonClickEvent -> {
            String appName = appNameText.getValue();
            String[] orgIds = orgIdsText.getValue().trim().split("[,;\\s]*");
            String[] serviceAccounts = serviceAccountsText.getValue().trim().split("[,;\\s]*");
            String[] owners = ownersText.getValue().trim().split("[,;\\s]*");
            String[] roles = rolesText.getValue().trim().split("[,;\\s]*");
            String[] authorities = authoritiesText.getValue().trim().split("[,;\\s]*");
            try {
                String token = authenticationService.generateCredentials(UUID.randomUUID().toString(), orgIds, serviceAccounts, roles, authorities, appName, owners);
                tokenTextArea.setValue(token);
            } catch (Exception ignore) {

            }

        });
        content.add(appNameText);
        content.add(ownersText);
        content.add(orgIdsText);
        content.add(serviceAccountsText);
        content.add(rolesText);
        content.add(authoritiesText);
        content.add(tokenTextArea);
        buttons.add(generateBtn);
        buttons.add(new Button("Clear", buttonClickEvent -> {
            clearForm();
        }));
        content.add(buttons);
        return content;
    }

    private void clearForm() {
        appNameText.clear();
        ownersText.clear();
        orgIdsText.clear();
        serviceAccountsText.clear();
        rolesText.clear();
        authoritiesText.clear();
        tokenTextArea.clear();
    }
}

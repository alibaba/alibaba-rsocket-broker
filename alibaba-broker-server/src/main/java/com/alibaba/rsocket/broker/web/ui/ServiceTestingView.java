package com.alibaba.rsocket.broker.web.ui;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.metadata.GSVRoutingMetadata;
import com.alibaba.rsocket.metadata.MessageMimeTypeMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerHandlerRegistry;
import com.alibaba.spring.boot.rsocket.broker.responder.RSocketBrokerResponderHandler;
import com.alibaba.spring.boot.rsocket.broker.route.ServiceRoutingSelector;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.util.ByteBufPayload;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;

import static com.alibaba.rsocket.broker.web.ui.ServiceTestingView.NAV;


/**
 * Service testing View
 *
 * @author leijuan
 */
@Route(value = NAV, layout = MainLayout.class)
public class ServiceTestingView extends VerticalLayout {
    public static final String NAV = "ServiceTestingView";
    private RSocketBrokerHandlerRegistry handlerRegistry;
    private ServiceRoutingSelector routingSelector;
    private TextField serviceNameFiled;
    private TextField methodNameField;

    public ServiceTestingView(@Autowired RSocketBrokerHandlerRegistry handlerRegistry, @Autowired ServiceRoutingSelector routingSelector) {
        this.handlerRegistry = handlerRegistry;
        this.routingSelector = routingSelector;
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        VerticalLayout serviceCallForm = makeServiceCallForm();
        // Compose layout
        horizontalLayout.add(serviceCallForm);
        add(horizontalLayout);
    }

    VerticalLayout makeServiceCallForm() {
        VerticalLayout content = new VerticalLayout();
        this.serviceNameFiled = new TextField("Service Name");
        serviceNameFiled.setWidth("300px");
        this.methodNameField = new TextField("Method Name");
        methodNameField.setWidth("300px");
        TextArea jsonDataTextArea = new TextArea("JSON Data");
        jsonDataTextArea.setWidth("600px");
        jsonDataTextArea.setHeight("200px");
        Pre responsePre = new Pre();
        HorizontalLayout buttons = new HorizontalLayout();
        Button callButton = new Button("Invoke", buttonClickEvent -> {
            String serviceName = serviceNameFiled.getValue();
            String methodName = methodNameField.getValue();
            String jsonData = jsonDataTextArea.getValue();
            if (serviceName == null || serviceName.isEmpty()) {
                serviceNameFiled.setErrorMessage("Please input service name");
            }
            if (methodName == null || methodName.isEmpty()) {
                methodNameField.setErrorMessage("Please input service name");
            }
            if (jsonData != null) {
                jsonData = jsonData.trim();
                if (!jsonData.isEmpty() && !jsonData.startsWith("[")) {
                    jsonData = "[" + jsonData + "]";
                }
            }
            callRSocketService(serviceName, methodName, jsonData, responsePre);
        });
        content.add(new H3("RSocket Service Testing"));
        content.add(serviceNameFiled);
        content.add(methodNameField);
        content.add(jsonDataTextArea);
        content.add(new H4("Response"));
        content.add(responsePre);
        buttons.add(callButton);
        buttons.add(new Button("Clear", buttonClickEvent -> {
            serviceNameFiled.clear();
            serviceNameFiled.setInvalid(false);
            methodNameField.clear();
            jsonDataTextArea.clear();
            responsePre.setText("");
        }));
        content.add(buttons);
        return content;
    }

    public void callRSocketService(String service, String method, @Nullable String jsonData, Pre response) {
        Integer handlerId = this.routingSelector.findHandler(ServiceLocator.serviceHashCode(service));
        if (handlerId != null) {
            RSocketBrokerResponderHandler handler = handlerRegistry.findById(handlerId);
            if (handler != null) {
                //composite metadata for health check
                RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(
                        new GSVRoutingMetadata(null, service, method, null),
                        new MessageMimeTypeMetadata(RSocketMimeType.Json));
                ByteBuf payLoadData;
                if (jsonData == null || jsonData.isEmpty()) {
                    payLoadData = Unpooled.EMPTY_BUFFER;
                } else {
                    payLoadData = Unpooled.wrappedBuffer(jsonData.getBytes(StandardCharsets.UTF_8));
                }
                Payload requestPayload = ByteBufPayload.create(payLoadData, compositeMetadata.getContent());
                handler.getPeerRsocket().requestResponse(requestPayload)
                        .doOnError(throwable -> getUI().ifPresent(ui -> ui.access(() -> {
                            response.setText(throwable.getMessage());
                        })))
                        .subscribe(payload -> getUI().ifPresent(ui -> ui.access(() -> {
                            response.setText(payload.getDataUtf8());
                        })));
            } else {
                this.serviceNameFiled.setInvalid(true);
                this.serviceNameFiled.setErrorMessage("No Service Provider!");
            }
        } else {
            this.serviceNameFiled.setInvalid(true);
            this.serviceNameFiled.setErrorMessage("Service not found!");
        }

    }
}

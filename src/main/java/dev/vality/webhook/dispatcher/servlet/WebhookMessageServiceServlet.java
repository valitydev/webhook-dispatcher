package dev.vality.webhook.dispatcher.servlet;

import dev.vality.webhook.dispatcher.WebhookMessageServiceSrv;
import dev.vality.webhook.dispatcher.handler.WebhookMessageServiceHandler;
import dev.vality.woody.thrift.impl.http.THServiceBuilder;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
@WebServlet("/webhook-message-service")
public class WebhookMessageServiceServlet extends GenericServlet {

    private final WebhookMessageServiceHandler handler;

    private Servlet servlet;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        servlet = new THServiceBuilder()
                .build(WebhookMessageServiceSrv.Iface.class, handler);
    }

    @Override
    public void service(
            ServletRequest request,
            ServletResponse response) throws ServletException, IOException {
        servlet.service(request, response);
    }
}

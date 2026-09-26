// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.builtinWebServer.impl.webServer.liveReload;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.builtinWebServer.BuiltInServerManager;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.impl.jsonRpc.Client;
import consulo.builtinWebServer.impl.jsonRpc.MessageServer;
import consulo.builtinWebServer.impl.webSocket.WebSocketClient;
import consulo.builtinWebServer.impl.webSocket.WebSocketHandshakeHandler;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

@ExtensionImpl(id = "webServerPage")
public final class WebServerPageRequestHandler extends WebSocketHandshakeHandler {
    private static final MessageServer MESSAGE_SERVER = (client, message) -> {
    };

    private final WebServerPageConnectionService myConnectionService;

    @Inject
    public WebServerPageRequestHandler(BuiltInServerManager serverManager,
                                       ApplicationConcurrency applicationConcurrency,
                                       WebServerPageConnectionService connectionService) {
        super(serverManager, applicationConcurrency);
        myConnectionService = connectionService;
    }

    @Override
    public boolean isSupported(HttpRequest request) {
        return super.isSupported(request) && checkPrefix(request.uri(), WebServerPageConnectionService.RELOAD_WS_URL_PREFIX);
    }

    @Override
    protected MessageServer getMessageServer() {
        return MESSAGE_SERVER;
    }

    @Override
    public void connected(Client client, @Nullable Map<String, List<String>> parameters) {
        if (client instanceof WebSocketClient webSocketClient) {
            myConnectionService.connected(webSocketClient, parameters, "");
        }
    }

    @Override
    public void disconnected(Client client) {
        if (client instanceof WebSocketClient webSocketClient) {
            myConnectionService.disconnected(webSocketClient);
        }
    }
}

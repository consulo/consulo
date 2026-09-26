/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.web.internal.servlet;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.internal.ApplicationEx;
import consulo.builtinWebServer.impl.webServer.liveReload.WebServerPageConnectionService;
import consulo.logging.Logger;
import jakarta.servlet.ServletContext;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-09-25
 */
public class WebServerPageEndpoint extends Endpoint {
    private static final String TOKEN_PATH_PARAMETER = "token";

    private static final String RELOAD_WS_URL_PREFIX = "jb-server-page";

    public static final String PATH =
        "/" + BuiltInWebServerServlet.RELATIVE_PREFIX + "{" + TOKEN_PATH_PARAMETER + "}/" + RELOAD_WS_URL_PREFIX;

    private static final Logger LOG = Logger.getInstance(WebServerPageEndpoint.class);

    private static final long HEARTBEAT_DELAY = TimeUnit.SECONDS.toMillis(25);

    private final ServletContext myServletContext;

    private @Nullable WebServerPageConnectionService myService;
    private @Nullable JakartaWebSocketConnection myConnection;
    private @Nullable ScheduledFuture<?> myHeartbeat;

    public WebServerPageEndpoint(ServletContext servletContext) {
        myServletContext = servletContext;
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        String token = session.getPathParameters().get(TOKEN_PATH_PARAMETER);
        Application application = ApplicationManager.getApplication();
        if (token == null
            || application == null
            || !((ApplicationEx) application).isLoaded()
            || !BuiltInWebServerSessionTokens.get(myServletContext).isValid(token)) {
            close(session);
            return;
        }

        WebServerPageConnectionService service = WebServerPageConnectionService.getInstance();
        JakartaWebSocketConnection connection = new JakartaWebSocketConnection(session);
        ScheduledFuture<?> heartbeat = application.getInstance(ApplicationConcurrency.class)
            .getScheduledExecutorService()
            .scheduleWithFixedDelay(connection::sendHeartbeat, HEARTBEAT_DELAY, HEARTBEAT_DELAY, TimeUnit.MILLISECONDS);
        synchronized (this) {
            myService = service;
            myConnection = connection;
            myHeartbeat = heartbeat;
        }

        String referrerPrefix = BuiltInWebServerServlet.prefix(myServletContext.getContextPath(), token);
        service.connected(connection, session.getRequestParameterMap(), referrerPrefix);
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        disconnect();
    }

    @Override
    public void onError(Session session, Throwable throwable) {
        if (throwable instanceof IOException) {
            LOG.debug(throwable);
        }
        else {
            LOG.error(throwable);
        }
        disconnect();
    }

    private void disconnect() {
        WebServerPageConnectionService service;
        JakartaWebSocketConnection connection;
        ScheduledFuture<?> heartbeat;
        synchronized (this) {
            service = myService;
            connection = myConnection;
            heartbeat = myHeartbeat;
            myService = null;
            myConnection = null;
            myHeartbeat = null;
        }

        if (heartbeat != null) {
            heartbeat.cancel(false);
        }

        if (service != null && connection != null) {
            service.disconnected(connection);
        }
    }

    private static void close(Session session) {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, null));
        }
        catch (IOException e) {
            LOG.debug(e);
        }
    }
}

// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint;

import consulo.endpoint.localize.EndpointLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * Describes available types of endpoints.
 */
public final class EndpointType {
    public static final EndpointType XML_WEB_SERVICE_TYPE =
        new EndpointType("XML-Web-Service", PlatformIconGroup.webreferencesServer(), EndpointLocalize.endpointTypeXmlWebService());

    public static final EndpointType HTTP_SERVER_TYPE =
        new EndpointType("HTTP-Server", PlatformIconGroup.webreferencesServer(), EndpointLocalize.endpointTypeHttpServer());

    public static final EndpointType HTTP_CLIENT_TYPE =
        new EndpointType("HTTP-Client", PlatformIconGroup.javaeeWebserviceclient(), EndpointLocalize.endpointTypeHttpClient());

    public static final EndpointType HTTP_MOCK_TYPE =
        new EndpointType("HTTP-Mock-Server", PlatformIconGroup.webreferencesServer(), EndpointLocalize.endpointTypeHttpMockServer());

    public static final EndpointType WEBSOCKET_SERVER_TYPE =
        new EndpointType("WebSocket-Server", PlatformIconGroup.webreferencesServer(), EndpointLocalize.endpointTypeWebsocketServer());

    public static final EndpointType GRAPH_QL_TYPE =
        new EndpointType("Graph-QL", PlatformIconGroup.webreferencesServer(), EndpointLocalize.endpointTypeGraphQl());

    public static final EndpointType WEBSOCKET_CLIENT_TYPE =
        new EndpointType("WebSocket-Client", PlatformIconGroup.javaeeWebserviceclient(), EndpointLocalize.endpointTypeWebsocketClient());

    public static final EndpointType API_DEFINITION_TYPE =
        new EndpointType("API-Definition", PlatformIconGroup.filetypesConfig(), EndpointLocalize.endpointTypeApiDefinition());

    private final String myQueryTag;
    private final @Nullable Image myIcon;
    private final LocalizeValue myLocalizedMessage;

    public EndpointType(String queryTag, @Nullable Image icon, LocalizeValue localizedMessage) {
        myQueryTag = queryTag;
        myIcon = icon;
        myLocalizedMessage = localizedMessage;
    }

    /**
     * Identifier for search field of Endpoints View. Prefer Title-Case-With-Dashes format.
     */
    public String getQueryTag() {
        return myQueryTag;
    }

    public @Nullable Image getIcon() {
        return myIcon;
    }

    public LocalizeValue getLocalizedMessage() {
        return myLocalizedMessage;
    }
}

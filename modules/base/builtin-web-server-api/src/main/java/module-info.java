/**
 * @author VISTALL
 * @since 2022-09-13
 */
module consulo.builtin.web.server.api {
    requires transitive consulo.application.api;
    requires transitive consulo.http.api;

    exports consulo.builtinWebServer;
    exports consulo.builtinWebServer.custom;
    exports consulo.builtinWebServer.http;
    exports consulo.builtinWebServer.http.util;
    exports consulo.builtinWebServer.json;
    exports consulo.builtinWebServer.localize;
    exports consulo.builtinWebServer.webSocket;
    exports consulo.builtinWebServer.xml;
}
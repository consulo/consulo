/**
 * @author VISTALL
 * @since 13-Sep-22
 */
module consulo.builtin.web.server.api {
  requires transitive consulo.application.api;
  requires transitive consulo.http.api;

  exports consulo.builtinWebServer;
  exports consulo.builtinWebServer.http;
  exports consulo.builtinWebServer.http.util;
  exports consulo.builtinWebServer.json;
  exports consulo.builtinWebServer.webSocket;
  exports consulo.builtinWebServer.xml;
  exports consulo.builtinWebServer.custom;
}
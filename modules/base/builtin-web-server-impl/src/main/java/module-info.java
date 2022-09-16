/**
 * @author VISTALL
 * @since 16-Sep-22
 */
module consulo.builtin.web.server.impl {
  requires consulo.application.impl;
  requires consulo.builtin.web.server.api;
  requires consulo.project.ui.api;
  requires consulo.external.service.api;
  requires consulo.util.netty;
  requires com.google.common;

  requires xmlrpc.client;
  requires xmlrpc.common;
  requires xmlrpc.server;

  requires io.netty.buffer;
  requires io.netty.codec;
  requires io.netty.codec.http;
  requires io.netty.common;
  requires io.netty.handler;
  requires io.netty.resolver;
  requires io.netty.transport;

  exports consulo.builtinWebServer.impl to consulo.ide.impl;
  exports consulo.builtinWebServer.impl.http to consulo.desktop.ide.impl;

  opens consulo.builtinWebServer.impl to consulo.util.xml.serializer;
}
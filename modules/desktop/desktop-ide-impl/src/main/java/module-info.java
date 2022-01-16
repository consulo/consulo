/**
 * @author VISTALL
 * @since 16/01/2022
 */
open module consulo.desktop.ide.impl {
  requires consulo.ide.impl;

  requires com.sun.jna;
  requires com.sun.jna.platform;

  requires consulo.container.api;

  requires java.management;

  requires io.netty.buffer;
  requires io.netty.codec;
  requires io.netty.codec.http;
  requires io.netty.common;
  requires io.netty.handler;
  requires io.netty.resolver;
  requires io.netty.transport;

  exports consulo.desktop.application.util;
  exports consulo.desktop.container.impl;
  exports consulo.desktop.startup;
  exports consulo.desktop.util.windows;
  exports consulo.desktop.util.windows.defender;
}
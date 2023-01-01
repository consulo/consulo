/**
 * @author VISTALL
 * @since 16/01/2022
 */
module consulo.desktop.ide.impl {
  requires consulo.ide.impl;

  requires com.sun.jna;
  requires com.sun.jna.platform;
  requires consulo.builtin.web.server.impl;

  requires consulo.container.api;

  requires java.management;

  requires io.netty.buffer;
  requires io.netty.codec;
  requires io.netty.codec.http;
  requires io.netty.common;
  requires io.netty.handler;
  requires io.netty.resolver;
  requires io.netty.transport;

  exports consulo.desktop.application.util to consulo.ide.impl, consulo.desktop.awt.ide.impl, consulo.desktop.swt.ide.impl;
  exports consulo.desktop.container.impl to consulo.ide.impl, consulo.desktop.awt.ide.impl, consulo.desktop.swt.ide.impl;
  exports consulo.desktop.startup to consulo.ide.impl, consulo.desktop.awt.ide.impl, consulo.desktop.swt.ide.impl;
  exports consulo.desktop.util.windows to consulo.ide.impl, consulo.desktop.awt.ide.impl, consulo.desktop.swt.ide.impl;
  exports consulo.desktop.util.windows.defender to consulo.ide.impl, consulo.desktop.awt.ide.impl, consulo.desktop.swt.ide.impl;

  opens consulo.desktop.util.windows to com.sun.jna;
}
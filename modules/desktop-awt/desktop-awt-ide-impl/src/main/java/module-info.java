/**
 * @author VISTALL
 * @since 14/01/2022
 */
open module consulo.desktop.awt.ide.impl {
  requires java.desktop;
  requires java.management;

  requires miglayout;
  requires com.google.common;
  requires com.github.weisj.jsvg;
  requires com.google.gson;

  requires io.netty.buffer;
  requires io.netty.codec;
  requires io.netty.codec.http;
  requires io.netty.common;
  requires io.netty.handler;
  requires io.netty.resolver;
  requires io.netty.transport;

  requires cobra.core;
  requires net.sf.cssbox.jstyleparser;

  requires org.apache.commons.imaging;

  requires consulo.container.api;
  requires consulo.container.impl;
  requires consulo.bootstrap;
  requires consulo.platform.impl;
  requires consulo.ui.impl;
  requires consulo.ide.impl;
  requires consulo.desktop.ide.impl;
  requires consulo.util.jna;
  requires consulo.ui.ex.awt.api;
  requires consulo.file.editor.impl;
  requires consulo.desktop.bootstrap;
  requires consulo.application.ui.impl;
  requires consulo.language.editor.impl;
  requires consulo.code.editor.impl;
  requires consulo.execution.impl;
  requires consulo.diff.impl;
  requires consulo.version.control.system.impl;

  // TODO remove
  requires gnu.trove;

  requires pty4j;
  requires jediterm;

  requires com.formdev.flatlaf;

  requires consulo.desktop.awt.bootstrap;
  requires consulo.desktop.awt.hacking;
  requires consulo.desktop.awt.eawt.wrapper;

  provides consulo.ui.internal.UIInternal with consulo.desktop.awt.ui.impl.DesktopUIInternalImpl;
  provides consulo.platform.internal.PlatformInternal with consulo.desktop.awt.platform.impl.DesktopAWTPlatformInternalImpl;
  provides consulo.container.boot.ContainerStartup with consulo.desktop.awt.container.impl.DesktopAWTContainerStartupImpl;
  provides consulo.ui.ex.awtUnsafe.internal.TargetAWTFacade with consulo.desktop.awt.facade.DesktopAWTTargetAWTImpl;

  // FIXME it's will not work due different classloaders?
  provides javax.imageio.spi.ImageReaderSpi with consulo.desktop.awt.spi.CommonsImagingImageReaderSpi;

  provides org.cobraparser.css.DefaultCssFactory with consulo.desktop.awt.ui.impl.htmlView.ConsuloDefaultCssFactory;
  provides org.cobraparser.css.StandardColorProvider with consulo.desktop.awt.ui.impl.htmlView.ConsuloStandardColorProvider;
}
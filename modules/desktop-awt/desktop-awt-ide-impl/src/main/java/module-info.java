import consulo.application.ui.awt.internal.TargetAWTFacade;
import consulo.desktop.awt.facade.DesktopAwtTargetAWTImpl;

/**
 * @author VISTALL
 * @since 14/01/2022
 */
open module consulo.desktop.awt.ide.impl {
  requires java.desktop;
  requires java.management;

  requires com.sun.jna;
  requires com.sun.jna.platform;
  requires miglayout;
  requires com.google.common;
  requires svg.salamander;
  requires com.google.gson;

  requires io.netty.buffer;
  requires io.netty.codec;
  requires io.netty.codec.http;
  requires io.netty.common;
  requires io.netty.handler;
  requires io.netty.resolver;
  requires io.netty.transport;

  requires org.apache.commons.imaging;

  requires consulo.container.api;
  requires consulo.container.impl;
  requires consulo.bootstrap;
  requires consulo.platform.impl;
  requires consulo.ui.impl;
  requires consulo.ide.impl;
  requires consulo.desktop.ide.impl;

  requires consulo.desktop.awt.bootstrap;
  requires consulo.desktop.awt.hacking;
  requires consulo.desktop.awt.eawt.wrapper;

  provides consulo.ui.internal.UIInternal with consulo.desktop.awt.ui.impl.DesktopUIInternalImpl;
  provides consulo.platform.internal.PlatformInternal with consulo.desktop.awt.platform.impl.DesktopPlatformInternalImpl;
  provides consulo.container.boot.ContainerStartup with consulo.desktop.awt.container.impl.DesktopContainerStartup;
  provides TargetAWTFacade with DesktopAwtTargetAWTImpl;

  // TODO it's will not work due different classloaders?
  provides javax.imageio.spi.ImageReaderSpi with consulo.desktop.awt.spi.CommonsImagingImageReaderSpi;
}
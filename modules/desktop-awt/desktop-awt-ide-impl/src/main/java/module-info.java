/**
 * @author VISTALL
 * @since 2022-01-14
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

    requires jdk.xml.dom;
    requires cobra.core;
    requires net.sf.cssbox.jstyleparser;

    requires it.unimi.dsi.fastutil;

    requires org.apache.commons.imaging;

    requires consulo.application.ui.impl;
    requires consulo.bootstrap;
    requires consulo.code.editor.impl;
    requires consulo.container.api;
    requires consulo.desktop.bootstrap;
    requires consulo.desktop.ide.impl;
    requires consulo.diagram.api;
    requires consulo.diff.impl;
    requires consulo.execution.impl;
    requires consulo.external.service.impl;
    requires consulo.file.editor.impl;
    requires consulo.ide.impl;
    requires consulo.language.editor.impl;
    requires consulo.platform.impl;
    requires consulo.project.ui.impl;
    requires consulo.ui.ex.awt.api;
    requires consulo.ui.impl;
    requires consulo.util.jna;
    requires consulo.version.control.system.impl;

    // TODO remove
    requires gnu.trove;

    requires pty4j;
    requires jediterm;

    requires com.formdev.flatlaf;
    requires com.formdev.flatlaf.swingx;

    requires swingx.all;

    requires consulo.desktop.awt.bootstrap;
    requires consulo.desktop.awt.hacking;
    requires consulo.desktop.awt.eawt.wrapper;

    provides consulo.ui.internal.UIInternal with consulo.desktop.awt.ui.impl.DesktopUIInternalImpl;
    provides consulo.platform.internal.PlatformInternal with consulo.desktop.awt.platform.impl.DesktopAWTPlatformInternalImpl;
    provides consulo.container.boot.ContainerStartup with consulo.desktop.awt.container.impl.DesktopAWTContainerStartupImpl;
    provides consulo.ui.ex.awtUnsafe.internal.TargetAWTFacade with consulo.desktop.awt.facade.DesktopAWTTargetAWTImpl;

    // FIXME will it not work due to different classloaders?
    provides javax.imageio.spi.ImageReaderSpi with consulo.desktop.awt.spi.CommonsImagingImageReaderSpi;

    provides org.cobraparser.css.DefaultCssFactory with consulo.desktop.awt.ui.impl.htmlView.ConsuloDefaultCssFactory;
    provides org.cobraparser.css.StandardColorProvider with consulo.desktop.awt.ui.impl.htmlView.ConsuloStandardColorProvider;

    provides com.formdev.flatlaf.FlatDefaultsAddon with consulo.desktop.awt.ui.plaf2.flat.ConsuloFlatDefaultsAddon;

    exports consulo.desktop.awt.ui to consulo.desktop.awt.os.mac;
}
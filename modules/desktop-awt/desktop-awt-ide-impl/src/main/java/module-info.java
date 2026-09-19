/**
 * @author VISTALL
 * @since 2022-01-14
 */
@SuppressWarnings("module")
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
    requires consulo.desktop.awt.editor.impl;
    requires consulo.desktop.awt.ui.impl;
    requires consulo.language.editor.impl;
    requires consulo.platform.impl;
    requires consulo.project.ui.impl;
    requires consulo.ui.ex.awt.api;
    requires consulo.ui.ex.impl;
    requires consulo.ui.impl;
    requires consulo.util.jna;
    requires consulo.version.control.system.impl;
    requires consulo.version.control.system.api;
    requires consulo.execution.api;
    requires consulo.execution.coverage.api;
    requires consulo.find.api;
    requires consulo.application.impl;
    requires consulo.ide.api;
    requires consulo.version.control.system.log.api;
    requires consulo.external.service.api;
    requires consulo.web.browser.api;
    requires consulo.version.control.system.distributed.api;
    requires consulo.language.editor.refactoring.api;
    requires consulo.execution.debug.api;
    requires consulo.builtin.web.server.api;
    requires consulo.color.scheme.ui.api;
    requires consulo.navigation.bar.api;
    requires consulo.language.ui.api;
    requires consulo.navigation.bar.impl;

    requires consulo.annotation;
    requires consulo.application.api;
    requires consulo.application.content.api;
    requires consulo.application.ui.api;
    requires consulo.base.icon.library;
    requires consulo.datacontext.api;
    requires consulo.disposer.api;
    requires consulo.language.api;
    requires consulo.language.editor.api;
    requires consulo.local.history.api;
    requires consulo.localize.api;
    requires consulo.logging.api;
    requires consulo.navigation.api;
    requires consulo.project.api;
    requires consulo.project.ui.view.api;
    requires consulo.ui.api;
    requires consulo.ui.ex.api;
    requires consulo.util.dataholder;
    requires consulo.util.lang;
    requires consulo.virtual.file.status.api;
    requires consulo.virtual.file.system.api;

    requires jakarta.inject;

    // TODO remove
    requires gnu.trove;

    requires pty4j;
    requires jediterm.core;
    requires jediterm.ui;

    requires com.formdev.flatlaf;
    requires com.formdev.flatlaf.swingx;

    requires swingx.all;

    requires jetbrains.runtime.api;
    
    requires consulo.desktop.awt.bootstrap;
    requires consulo.desktop.awt.hacking;
    requires consulo.desktop.awt.eawt.wrapper;

    provides consulo.platform.internal.PlatformInternal with consulo.desktop.awt.platform.impl.DesktopAWTPlatformInternalImpl;
    provides consulo.container.boot.ContainerStartup with consulo.desktop.awt.container.impl.DesktopAWTContainerStartupImpl;

    exports consulo.desktop.awt.ui to consulo.desktop.awt.os.mac;
}

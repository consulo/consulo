/**
 * @author VISTALL
 * @since 2026-08-16
 */
module consulo.desktop.qt.ide.impl {
    requires consulo.ide.impl;
    requires consulo.ui.ex.impl;
    requires consulo.ui.impl;
    requires consulo.desktop.ide.impl;
    requires consulo.platform.impl;
    requires consulo.bootstrap;
    requires consulo.application.ui.impl;
    requires consulo.project.ui.impl;
    requires consulo.language.editor.api;
    requires consulo.application.impl;
    requires consulo.code.editor.impl;
    requires consulo.language.editor.impl;
    requires consulo.navigation.bar.api;
    requires consulo.navigation.bar.impl;
    requires consulo.base.icon.library;
    requires consulo.execution.debug.api;

    // TargetAWTFacade is typed against java.awt even on a frontend which never draws with it
    requires java.desktop;

    requires qtjambi;

    provides consulo.ui.internal.UIInternal with consulo.desktop.qt.ui.impl.DesktopQtUIInternalImpl;
    provides consulo.platform.internal.PlatformInternal with consulo.desktop.qt.platform.DesktopQtPlatformInternal;
    provides consulo.container.boot.ContainerStartup with consulo.desktop.qt.container.boot.DesktopQtContainerStartup;
    provides consulo.ui.ex.awtUnsafe.internal.TargetAWTFacade with consulo.desktop.qt.ui.impl.TargetAWTFacadeStub;
}

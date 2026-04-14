/**
 * @author VISTALL
 * @since 11-Jul-22
 */
module consulo.desktop.swt.ide.impl {
  requires consulo.ide.impl;
  requires consulo.ui.impl;
  requires consulo.desktop.ide.impl;
  requires consulo.platform.impl;
  requires consulo.bootstrap;
  requires consulo.application.ui.impl;
  requires consulo.project.ui.impl;

  // TODO this impl is wrong for swt impl
  requires java.desktop;

  requires org.eclipse.swt;
  requires org.eclipse.swt.win32.win32.x86_64;
    requires consulo.language.editor.api;
    requires consulo.application.impl;
    requires consulo.code.editor.impl;
    requires consulo.language.editor.impl;

    provides consulo.ui.internal.UIInternal with consulo.desktop.swt.ui.impl.DesktopSwtUIInternalImpl;
  provides consulo.platform.internal.PlatformInternal with consulo.desktop.swt.platform.DesktopSwtPlatformInternal;
  provides consulo.container.boot.ContainerStartup with consulo.desktop.swt.container.boot.DesktopSwtContainerStartup;
  provides consulo.ui.ex.awtUnsafe.internal.TargetAWTFacade with consulo.desktop.swt.ui.impl.TargetAWTFacadeStub;
}
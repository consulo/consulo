/**
 * The qt ui layer - the component delegates, the awt bridge and the platform ui services behind them.
 * Sits below the editor so both it and the ide layer can build on the same widgets.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
module consulo.desktop.qt.ui.impl {
    requires consulo.ui.impl;
    requires consulo.ui.ex.impl;
    requires consulo.platform.impl;
    requires consulo.application.impl;
    requires consulo.application.ui.impl;
    requires consulo.project.ui.impl;

    // TargetAWTFacade is typed against java.awt even on a frontend which never draws with it
    requires java.desktop;

    requires qtjambi;

    exports consulo.desktop.qt.ui.impl;
    exports consulo.desktop.qt.ui.impl.action;
    exports consulo.desktop.qt.ui.impl.base;
    exports consulo.desktop.qt.ui.impl.clipboard;
    exports consulo.desktop.qt.ui.impl.dnd;
    exports consulo.desktop.qt.ui.impl.font;
    exports consulo.desktop.qt.ui.impl.htmlView;
    exports consulo.desktop.qt.ui.impl.image;
    exports consulo.desktop.qt.ui.impl.layout;
    exports consulo.desktop.qt.ui.impl.titleless;

    provides consulo.ui.internal.UIInternal with consulo.desktop.qt.ui.impl.DesktopQtUIInternalImpl;
    provides consulo.ui.ex.awtUnsafe.internal.TargetAWTFacade with consulo.desktop.qt.ui.impl.TargetAWTFacadeStub;
}

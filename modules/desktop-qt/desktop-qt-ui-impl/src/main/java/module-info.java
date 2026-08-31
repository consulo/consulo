/**
 * The qt ui layer - the component delegates, the awt bridge and the platform ui services behind them.
 * Sits below the editor so both it and the ide layer can build on the same widgets.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
module consulo.desktop.qt.ui.impl {
    requires consulo.annotation;
    requires consulo.application.api;
    requires consulo.color.scheme.api;
    requires consulo.component.api;
    requires consulo.datacontext.api;
    requires consulo.disposer.api;
    requires consulo.localize.api;
    requires consulo.logging.api;
    requires consulo.platform.api;
    requires consulo.project.ui.api;
    requires consulo.project.ui.impl;
    requires consulo.ui.api;
    requires consulo.ui.ex.api;
    requires consulo.ui.ex.awt.api;
    requires consulo.util.dataholder;
    requires consulo.util.io;
    requires consulo.util.lang;
    requires consulo.base.icon.library;
    requires consulo.base.localize.library;
    requires consulo.ui.impl;
    requires consulo.ui.ex.impl;
    requires consulo.application.impl;

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

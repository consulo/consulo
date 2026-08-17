/**
 * The qt editor surface - the widget, the view layer and the models behind them.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
module consulo.desktop.qt.editor.impl {
    requires consulo.annotation;
    requires consulo.ide.impl;
    requires consulo.desktop.ide.impl;
    requires consulo.desktop.qt.ui.impl;
    requires consulo.ui.impl;
    requires consulo.ui.ex.impl;
    requires consulo.platform.impl;
    requires consulo.application.impl;
    requires consulo.application.ui.impl;
    requires consulo.project.ui.impl;
    requires consulo.code.editor.impl;
    requires consulo.language.editor.api;
    requires consulo.language.editor.impl;
    requires consulo.execution.debug.api;

    // TargetAWTFacade is typed against java.awt even on a frontend which never draws with it
    requires java.desktop;

    requires qtjambi;

    exports consulo.desktop.qt.editor.impl.internal to
        consulo.desktop.qt.ide.impl;
}

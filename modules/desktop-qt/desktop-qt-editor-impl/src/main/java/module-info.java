/**
 * The qt editor surface - the widget, the view layer and the models behind them.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
module consulo.desktop.qt.editor.impl {
    requires consulo.language.editor.ui.api;
    requires consulo.application.api;
    requires consulo.code.editor.api;
    requires consulo.color.scheme.api;
    requires consulo.datacontext.api;
    requires consulo.disposer.api;
    requires consulo.document.api;
    requires consulo.language.api;
    requires consulo.logging.api;
    requires consulo.navigation.api;
    requires consulo.platform.api;
    requires consulo.project.api;
    requires consulo.ui.api;
    requires consulo.ui.ex.api;
    requires consulo.ui.ex.awt.api;
    requires consulo.undo.redo.api;
    requires consulo.util.lang;
    requires consulo.base.icon.library;
    requires consulo.annotation;
    requires consulo.ide.impl;
    requires consulo.desktop.qt.ui.impl;
    requires consulo.ui.ex.impl;

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

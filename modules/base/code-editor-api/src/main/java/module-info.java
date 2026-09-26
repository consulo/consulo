import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2022-02-05
 */
@NullMarked
@SuppressWarnings("module")
module consulo.code.editor.api {
    // todo not required dependency
    requires java.desktop;

    requires transitive consulo.application.api;
    requires transitive consulo.project.api;
    requires transitive consulo.virtual.file.system.api;
    requires transitive consulo.document.api;
    requires transitive consulo.ui.ex.api;
    requires transitive consulo.color.scheme.api;
    requires transitive consulo.undo.redo.api;

    exports consulo.codeEditor;
    exports consulo.codeEditor.imaginary;
    exports consulo.codeEditor.action;
    exports consulo.codeEditor.event;
    exports consulo.codeEditor.localize;
    exports consulo.codeEditor.markup;
    exports consulo.codeEditor.util;
    exports consulo.codeEditor.util.popup;
    exports consulo.codeEditor.toolbar.floating;

    exports consulo.codeEditor.internal.stickyLine to
        consulo.code.editor.impl,
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.ide.impl,
        consulo.language.editor.impl;

    exports consulo.codeEditor.internal to
        consulo.code.editor.impl,
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.qt.editor.impl,
        consulo.desktop.qt.ide.impl,
        consulo.desktop.qt.ui.impl,
        consulo.execution.api,
        consulo.execution.debug.impl,
        consulo.file.editor.api,
        consulo.ide.impl,
        consulo.it,
        consulo.language.code.style.ui.api,
        consulo.language.code.style.impl,
        consulo.language.editor.api,
        consulo.language.editor.impl,
        consulo.language.editor.refactoring.api,
        consulo.language.editor.ui.api,
        consulo.language.inject.impl,
        consulo.ui.ex.impl,
        consulo.version.control.system.impl,
        consulo.web.editor.impl,
        consulo.web.ide,
        consulo.web.ui.impl;

    opens consulo.codeEditor.internal to consulo.proxy;
}
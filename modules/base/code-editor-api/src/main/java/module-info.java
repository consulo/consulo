/**
 * @author VISTALL
 * @since 05/02/2022
 */
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
    exports consulo.codeEditor.action;
    exports consulo.codeEditor.event;
    exports consulo.codeEditor.localize;
    exports consulo.codeEditor.markup;
    exports consulo.codeEditor.util;
    exports consulo.codeEditor.util.popup;

    exports consulo.codeEditor.internal.stickyLine to
        consulo.desktop.awt.ide.impl;

    exports consulo.codeEditor.internal to consulo.ide.impl,
        consulo.code.editor.impl,
        consulo.language.inject.impl,
        consulo.desktop.awt.ide.impl,
        consulo.language.editor.api,
        consulo.language.code.style.ui.api,
        consulo.execution.api,
        consulo.language.editor.refactoring.api,
        consulo.language.editor.ui.api,
        consulo.file.editor.api,
        consulo.desktop.swt.ide.impl,
        consulo.language.editor.impl,
        consulo.execution.debug.impl;
}
/**
 * @author VISTALL
 * @since 2022-02-15
 */
@SuppressWarnings("module")
module consulo.document.impl {
    requires transitive consulo.application.api;
    requires transitive consulo.project.api;
    requires transitive consulo.document.api;
    requires transitive consulo.undo.redo.api;

    requires it.unimi.dsi.fastutil;

    exports consulo.document.impl to
        consulo.code.editor.impl,
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.file.editor.impl,
        consulo.ide.impl,
        consulo.language.editor.impl,
        consulo.language.impl,
        consulo.test.impl;

    exports consulo.document.impl.event to
        consulo.code.editor.impl,
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.ide.impl,
        consulo.language.impl,
        consulo.language.editor.impl;
}
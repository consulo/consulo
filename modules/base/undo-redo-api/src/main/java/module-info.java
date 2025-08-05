/**
 * @author VISTALL
 * @since 2022-02-16
 */
module consulo.undo.redo.api {
    requires transitive consulo.document.api;
    requires transitive consulo.project.api;
    requires transitive consulo.util.lang;

    exports consulo.undoRedo;
    exports consulo.undoRedo.builder;
    exports consulo.undoRedo.event;
    exports consulo.undoRedo.util;

    exports consulo.undoRedo.internal to
        consulo.ide.impl,
        consulo.language.editor.refactoring.api,
        consulo.local.history.impl,
        consulo.code.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.os.mac;

    exports consulo.undoRedo.internal.builder to
        consulo.desktop.awt.ide.impl,
        consulo.diff.impl,
        consulo.ide.impl;
}
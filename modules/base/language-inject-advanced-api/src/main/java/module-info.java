/**
 * @author VISTALL
 * @since 2025-01-12
 */
module consulo.language.inject.advanced.api {
    requires transitive consulo.language.api;
    requires transitive consulo.language.editor.api;
    requires transitive consulo.language.editor.ui.api;

    requires consulo.undo.redo.api;

    // TODO remove in future
    requires java.desktop;
    requires consulo.ui.ex.awt.api;
    requires forms.rt;

    exports consulo.language.inject.advanced;
    exports consulo.language.inject.advanced.ui;
    exports consulo.language.inject.advanced.pattern;

    exports consulo.language.inject.advanced.internal to consulo.language.inject.advanced.impl;
}
/**
 * @author VISTALL
 * @since 2025-01-09
 */
module consulo.configuration.editor.api {
    requires transitive consulo.file.editor.api;
    requires transitive consulo.project.api;

    exports consulo.configuration.editor;

    exports consulo.configuration.editor.internal to consulo.configuration.editor.impl;
}
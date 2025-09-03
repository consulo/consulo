/**
 * @author VISTALL
 * @since 2025-09-02
 */
module consulo.language.diagram.api {
    requires transitive consulo.diagram.api;
    requires transitive consulo.language.api;

    exports consulo.language.editor.diagram;
}
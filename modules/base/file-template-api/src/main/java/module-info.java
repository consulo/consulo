/**
 * @author VISTALL
 * @since 27-Mar-22
 */
module consulo.file.template.api {
    requires transitive consulo.project.api;
    requires transitive consulo.language.api;
    requires transitive consulo.language.code.style.api;
    requires transitive consulo.undo.redo.api;

    exports consulo.fileTemplate;
}
/**
 * @author VISTALL
 * @since 2022-03-27
 */
module consulo.file.template.api {
    requires transitive consulo.language.api;
    requires transitive consulo.language.code.style.api;
    requires transitive consulo.project.api;
    requires transitive consulo.undo.redo.api;

    exports consulo.fileTemplate;
    exports consulo.fileTemplate.localize;
}
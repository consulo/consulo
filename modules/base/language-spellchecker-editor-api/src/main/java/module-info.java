/**
 * @author VISTALL
 * @since 2023-03-26
 */
module consulo.language.spellchecker.editor.api {
    requires transitive consulo.language.spellchecker.api;
    requires transitive consulo.language.editor.api;

    exports consulo.language.spellchecker.editor;
    exports consulo.language.spellchecker.editor.inspection;
}
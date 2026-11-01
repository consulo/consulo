/**
 * @author VISTALL
 * @since 2023-03-26
 */
module consulo.language.spellchecker.editor.api {
    requires transitive consulo.language.spellchecker.api;
    requires transitive consulo.language.editor.api;
    requires transitive consulo.configurable.api;

    exports consulo.language.spellchecker.editor;
    exports consulo.language.spellchecker.editor.dictionary;
    exports consulo.language.spellchecker.editor.inspection;
    exports consulo.language.spellchecker.editor.localize;
}

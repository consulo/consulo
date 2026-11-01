/**
 * @author VISTALL
 * @since 2023-03-26
 */
module consulo.language.spellchecker.editor.impl {
    requires transitive consulo.language.spellchecker.api;
    requires transitive consulo.language.spellchecker.editor.api;

    requires consulo.language.editor.refactoring.api;
    requires consulo.file.chooser.api;
    requires consulo.ui.ex.api;

    opens consulo.language.spellchecker.editor.impl to consulo.util.xml.serializer;
    opens consulo.language.spellchecker.editor.impl.inspection to consulo.util.xml.serializer;
    opens consulo.language.spellchecker.editor.impl.setting to consulo.util.xml.serializer;
    opens consulo.language.spellchecker.editor.impl.state to consulo.util.xml.serializer, consulo.component.impl;
}

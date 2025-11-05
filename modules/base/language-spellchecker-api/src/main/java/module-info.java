/**
 * @author VISTALL
 * @since 2023-01-14
 */
module consulo.language.spellchecker.api {
    requires transitive consulo.language.api;

    exports consulo.language.spellcheker;
    exports consulo.language.spellcheker.tokenizer;
    exports consulo.language.spellcheker.tokenizer.splitter;

    exports consulo.language.spellcheker.internal to consulo.language.spellchecker.impl;
}
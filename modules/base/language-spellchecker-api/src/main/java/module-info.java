/**
 * @author VISTALL
 * @since 14/01/2023
 */
module consulo.language.spellchecker.api {
  requires transitive consulo.language.api;

  exports consulo.language.spellcheker;
  exports consulo.language.spellcheker.tokenizer;
  exports consulo.language.spellcheker.tokenizer.splitter;

  exports consulo.language.spellcheker.internal to consulo.language.spellchecker.impl;
}
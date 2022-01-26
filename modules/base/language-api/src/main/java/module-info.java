/**
 * @author VISTALL
 * @since 22/01/2022
 */
module consulo.language.api {
  requires transitive consulo.module.content.api;
  requires transitive consulo.document.api;

  exports consulo.language;
  exports consulo.language.ast;
  exports consulo.language.content;
  exports consulo.language.psi;
  exports consulo.language.parser;
  exports consulo.language.lexer;
  exports consulo.language.file;
  exports consulo.language.psi.event;
  exports consulo.language.util;
  exports consulo.language.version;
  exports consulo.language.psi.resolve;
  exports consulo.language.template;
  exports consulo.language.psi.scope;
  exports consulo.language.psi.meta;
  exports consulo.language.psi.stub;
  exports consulo.language.psi.util;

  // TODO impl package
  exports consulo.language.psi.scope.internal;
}
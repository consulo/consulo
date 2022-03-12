/**
 * @author VISTALL
 * @since 22/01/2022
 */
module consulo.language.api {
  requires transitive consulo.module.content.api;
  requires transitive consulo.project.content.api;
  requires transitive consulo.document.api;
  requires transitive consulo.index.io;
  requires transitive consulo.navigation.api;
  requires consulo.ui.ex.api;

  exports consulo.language;
  exports consulo.language.ast;
  exports consulo.language.content;
  exports consulo.language.file;
  exports consulo.language.file.inject;
  exports consulo.language.file.light;
  exports consulo.language.lexer;
  exports consulo.language.parser;
  exports consulo.language.plain;
  exports consulo.language.plain.ast;
  exports consulo.language.plain.psi;
  exports consulo.language.icon;
  exports consulo.language.pattern;
  exports consulo.language.inject;
  exports consulo.language.pom;
  exports consulo.language.pom.event;
  exports consulo.language.psi;
  exports consulo.language.psi.event;
  exports consulo.language.pattern.compiler;
  exports consulo.language.psi.internal to consulo.ide.impl, consulo.language.impl;
  exports consulo.language.psi.meta;
  exports consulo.language.psi.resolve;
  exports consulo.language.psi.scope;
  exports consulo.language.psi.stub;
  exports consulo.language.psi.search;
  exports consulo.language.psi.search.scope;
  exports consulo.language.psi.stub.internal to consulo.ide.impl, consulo.language.impl;

  exports consulo.language.psi.util;
  opens consulo.language.psi.util to consulo.ide.impl;

  exports consulo.language.template;
  exports consulo.language.util;
  exports consulo.language.version;
}
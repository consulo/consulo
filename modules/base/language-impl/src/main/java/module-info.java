/**
 * @author VISTALL
 * @since 16-Feb-22
 */
module consulo.language.impl {
  requires consulo.project.api;
  requires consulo.language.api;
  requires consulo.document.impl;
  requires consulo.undo.redo.api;
  requires consulo.util.interner;

  exports consulo.language.impl;
  exports consulo.language.impl.ast;
  exports consulo.language.impl.parser;
  exports consulo.language.impl.file;
  exports consulo.language.impl.psi;
  exports consulo.language.impl.psi.pointer;
  exports consulo.language.impl.psi.stub;
  exports consulo.language.impl.psi.path;
  exports consulo.language.impl.psi.template;
  exports consulo.language.impl.util;

  // internal implementation
  exports consulo.language.impl.plain to consulo.ide.impl, consulo.test.impl;
  exports consulo.language.impl.internal.ast to consulo.ide.impl, consulo.test.impl, consulo.language.code.style.api;
  exports consulo.language.impl.internal.file to consulo.ide.impl, consulo.language.inject.impl;
  exports consulo.language.impl.internal.parser to consulo.ide.impl, consulo.test.impl;
  exports consulo.language.impl.internal.psi to
          consulo.ide.impl,
          consulo.test.impl,
          consulo.component.impl,
          consulo.language.code.style.api,
          consulo.language.editor.impl,
          consulo.language.inject.impl,
          consulo.util.xml.serializer;
  exports consulo.language.impl.internal.psi.diff to consulo.ide.impl, consulo.language.inject.impl;
  exports consulo.language.impl.internal.psi.pointer to consulo.ide.impl, consulo.test.impl, consulo.language.inject.impl;
  exports consulo.language.impl.internal.psi.stub to consulo.ide.impl;
  exports consulo.language.impl.internal.pom to consulo.ide.impl;
  exports consulo.language.impl.internal.pattern.compiler to consulo.ide.impl;
  exports consulo.language.impl.internal.template to consulo.ide.impl;
}
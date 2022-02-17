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
  exports consulo.language.impl.ast.internal to consulo.ide.impl;
  exports consulo.language.impl.file;
  exports consulo.language.impl.file.internal to consulo.ide.impl;
  exports consulo.language.impl.parser.internal to consulo.ide.impl;
  exports consulo.language.impl.psi;
  exports consulo.language.impl.psi.internal to consulo.ide.impl;
  exports consulo.language.impl.psi.internal.diff to consulo.ide.impl;
  exports consulo.language.impl.psi.internal.pointer to consulo.ide.impl;
  exports consulo.language.impl.psi.reference;
  exports consulo.language.impl.psi.stub;
}
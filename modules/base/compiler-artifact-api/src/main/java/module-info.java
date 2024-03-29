/**
 * @author VISTALL
 * @since 19-Feb-22
 */
module consulo.compiler.artifact.api {
  requires transitive consulo.compiler.api;
  requires transitive consulo.module.api;
  requires transitive consulo.module.content.api;
  requires consulo.execution.api;
  requires transitive consulo.ui.ex.awt.api;

  exports consulo.compiler.artifact;
  exports consulo.compiler.artifact.event;
  exports consulo.compiler.artifact.element;
  exports consulo.compiler.artifact.ui;
  exports consulo.compiler.artifact.ui.awt;
  exports consulo.compiler.artifact.execution;

  opens consulo.compiler.artifact.element to consulo.util.xml.serializer;

  exports consulo.compiler.artifact.internal to
    consulo.compiler.artifact.impl,
    consulo.compiler.impl,
    consulo.ide.impl;
}
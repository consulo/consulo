/**
 * @author VISTALL
 * @since 2023-11-13
 */
module consulo.compiler.artifact.impl {
  requires transitive consulo.compiler.artifact.api;

  requires consulo.component.impl;
  requires consulo.module.content.api;

  exports consulo.compiler.artifact.impl.internal to consulo.compiler.impl, consulo.ide.impl;

  opens consulo.compiler.artifact.impl.internal to
    consulo.component.impl,
    consulo.util.xml.serializer;

  opens consulo.compiler.artifact.impl.internal.state to
    consulo.component.impl,
    consulo.util.xml.serializer;
}
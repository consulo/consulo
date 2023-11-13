/**
 * @author VISTALL
 * @since 2023-11-13
 */
module consulo.build.ui.impl {
  requires transitive consulo.build.ui.api;
  requires consulo.compiler.api;

  exports consulo.build.ui.impl.internal.event to
    consulo.ide.impl,
    consulo.compiler.impl;

  exports consulo.build.ui.impl.internal to
    consulo.ide.impl,
    consulo.compiler.impl;
}
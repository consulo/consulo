/**
 * @author VISTALL
 * @since 12-Feb-22
 */
module consulo.compiler.api {
  requires transitive consulo.module.api;
  requires transitive consulo.module.content.api;
  requires transitive consulo.navigation.api;
  requires transitive consulo.project.ui.api;
  requires transitive consulo.index.io;

  exports consulo.compiler;
  exports consulo.compiler.event;
  exports consulo.compiler.generic;
  exports consulo.compiler.setting;
  exports consulo.compiler.scope;
  exports consulo.compiler.util;
}
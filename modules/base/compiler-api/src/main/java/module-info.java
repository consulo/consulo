/**
 * @author VISTALL
 * @since 12-Feb-22
 */
module consulo.compiler.api {
  requires transitive consulo.module.api;
  requires transitive consulo.navigation.api;
  requires transitive consulo.project.ui.api;

  exports consulo.compiler;
  exports consulo.compiler.event;
  exports consulo.compiler.setting;
}
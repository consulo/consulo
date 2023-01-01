/**
 * @author VISTALL
 * @since 10-Apr-22
 */
module consulo.path.macro.api {
  requires transitive consulo.application.api;
  requires transitive consulo.virtual.file.system.api;
  requires transitive consulo.datacontext.api;
  requires transitive consulo.project.api;
  requires transitive consulo.module.api;

  exports consulo.pathMacro;
}
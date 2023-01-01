/**
 * @author VISTALL
 * @since 03/02/2022
 */
module consulo.navigation.api {
  requires transitive consulo.application.api;
  requires transitive consulo.virtual.file.system.api;

  exports consulo.navigation;
}
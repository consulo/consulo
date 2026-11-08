/**
 * @author VISTALL
 * @since 07/01/2023
 */
module consulo.repository.ui.api {
  requires transitive consulo.application.api;
  requires transitive consulo.project.api;

  exports consulo.repository.ui;
  exports consulo.repository.ui.localize;
}

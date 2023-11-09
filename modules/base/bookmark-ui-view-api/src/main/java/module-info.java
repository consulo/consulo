/**
 * @author VISTALL
 * @since 2023-11-08
 */
module consulo.bookmark.ui.view.api {
  requires transitive consulo.bookmark.api;
  requires transitive consulo.project.ui.view.api;

  exports consulo.bookmark.ui.view.event;
  exports consulo.bookmark.ui.view;

  // TODO remove in future
  requires java.desktop;
}
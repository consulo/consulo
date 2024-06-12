/**
 * @author VISTALL
 * @since 20/01/2022
 */
module consulo.file.chooser.api {
  // TODO [VISTALL] obsolete dep
  requires java.desktop;

  requires transitive consulo.application.api;
  requires transitive consulo.virtual.file.system.api;
  
  requires consulo.base.icon.library;

  exports consulo.fileChooser;
  exports consulo.fileChooser.provider;
  exports consulo.fileChooser.util;
}
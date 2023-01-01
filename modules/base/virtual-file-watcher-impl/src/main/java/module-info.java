/**
 * @author VISTALL
 * @since 12-Feb-22
 */
module consulo.virtual.file.watcher.impl {
  // TODO drop
  requires java.desktop;

  requires consulo.virtual.file.watcher.api;
  requires consulo.build.ui.api;

  opens consulo.virtualFileSystem.fileWatcher.impl to consulo.util.xml.serializer;
}
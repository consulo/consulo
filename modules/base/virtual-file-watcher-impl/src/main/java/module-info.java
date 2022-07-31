/**
 * @author VISTALL
 * @since 12-Feb-22
 */
module consulo.virtual.file.watcher.impl {
  // TODO drop
  requires java.desktop;
  requires forms.rt;

  requires transitive consulo.virtual.file.watcher.api;

  // TODO [VISTALL] very massive dependency, reduce it, when impl modules will introduced
  requires consulo.ide.impl;

  opens consulo.virtualFileSystem.fileWatcher.impl to consulo.util.xml.serializer;
}
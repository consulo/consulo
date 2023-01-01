/**
 * @author VISTALL
 * @since 12-Feb-22
 */
module consulo.virtual.file.watcher.api {
  requires transitive consulo.language.api;
  requires transitive consulo.virtual.file.system.api;
  requires transitive consulo.project.api;
  requires transitive consulo.execution.api;

  exports consulo.virtualFileSystem.fileWatcher;
}
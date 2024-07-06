/**
 * @author VISTALL
 * @since 04-Apr-22
 */
module consulo.diff.api {
  requires transitive consulo.project.api;
  requires transitive consulo.virtual.file.status.api;

  // TODO remove this dependencies in future
  requires java.desktop;
  requires transitive consulo.ui.ex.awt.api;
  requires transitive consulo.file.editor.api;

  exports consulo.diff;
  exports consulo.diff.chain;
  exports consulo.diff.localize;
  exports consulo.diff.merge;
  exports consulo.diff.dir;
  exports consulo.diff.request;
  exports consulo.diff.content;
  exports consulo.diff.fragment;
  exports consulo.diff.comparison;
  exports consulo.diff.comparison.iterable;
  exports consulo.diff.util;

  exports consulo.diff.internal to
    consulo.ide.impl,
    consulo.diff.impl,
    consulo.version.control.system.api,
    consulo.version.control.system.impl,
    consulo.desktop.awt.ide.impl;
}
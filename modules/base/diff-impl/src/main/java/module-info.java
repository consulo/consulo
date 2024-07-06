/**
 * @author VISTALL
 * @since 04-Jun-22
 */
module consulo.diff.impl {
  requires transitive consulo.diff.api;
  requires transitive consulo.language.api;
  requires transitive consulo.language.editor.api;
  requires consulo.process.api;

  // TODO remove in futures
  requires java.desktop;

  exports consulo.diff.impl.internal to
    consulo.ide.impl,
    consulo.desktop.awt.ide.impl;

  exports consulo.diff.impl.internal.action to
    consulo.ide.impl,
    consulo.desktop.awt.ide.impl;

  exports consulo.diff.impl.internal.editor to
    consulo.desktop.awt.ide.impl;

  exports consulo.diff.impl.internal.merge to
    consulo.desktop.awt.ide.impl;

  exports consulo.diff.impl.internal.fragment to
    consulo.ide.impl,
    consulo.desktop.awt.ide.impl;

  exports consulo.diff.impl.internal.dir to
    consulo.desktop.awt.ide.impl;

  exports consulo.diff.impl.internal.util to
    consulo.ide.impl,
    consulo.desktop.awt.ide.impl;

  exports consulo.diff.impl.internal.external to
    consulo.ide.impl,
    consulo.desktop.awt.ide.impl;

  opens consulo.diff.impl.internal.action to consulo.component.impl;
  opens consulo.diff.impl.internal to consulo.util.xml.serializer;
  opens consulo.diff.impl.internal.external to consulo.util.xml.serializer;
}
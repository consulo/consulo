/**
 * @author VISTALL
 * @since 04/02/2022
 */
module consulo.disposer.impl {
  requires transitive consulo.disposer.api;
  requires transitive consulo.util.lang;
  requires transitive consulo.util.collection;
  requires transitive consulo.util.interner;
  requires transitive consulo.logging.api;
  requires transitive consulo.hacking.java.base;

  provides consulo.disposer.internal.DisposerInternal with consulo.disposer.internal.impl.DisposerInternalImpl;
}
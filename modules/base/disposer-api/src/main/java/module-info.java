module consulo.disposer.api {
  requires transitive consulo.annotation;
  
  exports consulo.disposer;
  exports consulo.disposer.util;

  uses consulo.disposer.internal.DisposerInternal;

  // TODO [VISTALL] this package must be exported only to impl module!
  exports consulo.disposer.internal;
}
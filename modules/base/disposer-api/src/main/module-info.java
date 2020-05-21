module consulo.disposer.api {
  exports consulo.disposer;
  // TODO [VISTALL] this package must be exported only to impl module!
  exports consulo.disposer.internal;
  exports consulo.disposer.util;

  uses consulo.disposer.internal.DisposerInternal;
}
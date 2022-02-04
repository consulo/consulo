module consulo.disposer.api {
  requires transitive consulo.annotation;
  
  exports consulo.disposer;
  exports consulo.disposer.util;

  uses consulo.disposer.internal.DisposerInternal;

  exports consulo.disposer.internal to consulo.disposer.impl;
}
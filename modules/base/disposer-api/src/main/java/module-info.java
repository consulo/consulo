module consulo.disposer.api {
  requires transitive consulo.annotation;
  
  exports consulo.disposer;
  exports consulo.disposer.util;

  uses consulo.disposer.internal.DisposerInternal;
  uses consulo.disposer.internal.DiposerRegisterChecker;

  exports consulo.disposer.internal to consulo.disposer.impl, consulo.application.impl;
}
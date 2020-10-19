module consulo.injecting.api {
  requires transitive consulo.annotation;
  requires jakarta.inject;
  
  exports consulo.injecting;
  exports consulo.injecting.key;

  uses consulo.injecting.RootInjectingContainerFactory;
}
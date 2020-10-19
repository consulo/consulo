module consulo.util.lang {
  requires jdk.unsupported;
  requires consulo.annotation;
  requires consulo.logging.api;

  exports consulo.util.lang;
  exports consulo.util.lang.function;
  exports consulo.util.lang.ref;
  exports consulo.util.lang.reflect;
  exports consulo.util.lang.reflect.unsafe;
}
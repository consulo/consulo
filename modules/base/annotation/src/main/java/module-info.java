module consulo.annotation {
  requires transitive jakarta.annotation;
  requires transitive org.jspecify;
  requires transitive org.apiguardian.api;

  requires java.desktop;

  exports consulo.annotation;
  exports consulo.annotation.access;
  exports consulo.annotation.internal;
  exports consulo.annotation.component;

  exports org.intellij.lang.annotations;
  exports org.jetbrains.annotations;
}
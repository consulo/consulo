module consulo.annotation {
  requires transitive jakarta.annotation;
  requires java.desktop;

  exports consulo.annotation;
  exports consulo.annotation.access;
  exports consulo.annotation.internal;
  exports consulo.annotation.component;

  exports org.intellij.lang.annotations;
  exports org.jetbrains.annotations;
}
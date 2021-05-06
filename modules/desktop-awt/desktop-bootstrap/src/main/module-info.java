module consulo.desktop.bootstrap {
  requires consulo.container.api;
  requires consulo.container.impl;
  requires consulo.util.nodep;

  requires java.desktop;
  requires java.sql;
  requires jdk.unsupported;

  exports consulo.desktop.boot.main.windows;
}
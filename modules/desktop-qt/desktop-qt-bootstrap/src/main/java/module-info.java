module consulo.desktop.qt.bootstrap {
  requires consulo.container.api;
  requires consulo.util.nodep;

  requires java.desktop;
  requires java.sql;
  requires jdk.unsupported;

  requires qtjambi;

  exports consulo.desktop.qt.boot.main;
}

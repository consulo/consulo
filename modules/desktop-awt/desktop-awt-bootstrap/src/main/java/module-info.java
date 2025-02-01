module consulo.desktop.awt.bootstrap {
  requires consulo.container.api;
  requires consulo.util.nodep;

  requires java.desktop;

  // load bootstrap module, and later it will be used in plugins
  requires transitive consulo.desktop.bootstrap;
}
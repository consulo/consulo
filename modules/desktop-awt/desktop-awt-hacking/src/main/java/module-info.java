module consulo.desktop.awt.hacking {
  requires java.desktop;

  requires jsr305;
  requires consulo.util.lang;
  requires consulo.util.collection;
  requires consulo.platform.api;
  requires consulo.logging.api;

  exports consulo.awt.hacking;
}
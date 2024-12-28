module consulo.hacking.java.base {
  requires consulo.logging.api;

  exports consulo.hacking.java.base to consulo.disposer.impl, consulo.component.api;
}
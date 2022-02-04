module consulo.container.impl {
  requires consulo.container.api;
  requires consulo.util.nodep;

  // add temp dependency
  requires transitive java.scripting;
  // this dependency fix batik runtime
  requires transitive jdk.xml.dom;

  exports consulo.container.impl;
  exports consulo.container.impl.classloader;
  exports consulo.container.impl.classloader.proxy to consulo.proxy;
  exports consulo.container.impl.parser;

  uses consulo.container.boot.ContainerStartup;

  provides consulo.container.plugin.internal.PluginManagerInternal with consulo.container.impl.PluginManagerInternalImpl;
}
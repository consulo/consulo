module consulo.container.impl {
  requires consulo.container.api;
  requires consulo.util.nodep;

  // add temp dependency
  requires transitive java.scripting;
  // required by gson
  requires transitive java.sql;
  // this dependency fix batik runtime
  requires transitive jdk.xml.dom;
  // required consulo-util-lang
  requires transitive jdk.unsupported;

  exports consulo.container.impl;
  exports consulo.container.impl.classloader;
  exports consulo.container.impl.classloader.proxy to consulo.proxy;
  exports consulo.container.impl.parser;

  // FIXME this export not work as expected in multiclassloader system, but need for compilation, this export hardcoded in Java9ModuleInitializer
  exports consulo.container.impl.securityManager.impl to consulo.ide.impl;

  uses consulo.container.boot.ContainerStartup;

  provides consulo.container.plugin.internal.PluginManagerInternal with consulo.container.impl.PluginManagerInternalImpl;
}
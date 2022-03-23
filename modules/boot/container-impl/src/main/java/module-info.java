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

  exports consulo.container.impl to consulo.ide.impl, consulo.application.impl, consulo.component.impl;
  exports consulo.container.impl.classloader to consulo.ide.impl, consulo.application.impl;
  exports consulo.container.impl.classloader.proxy to consulo.proxy;
  exports consulo.container.impl.parser to consulo.ide.impl, consulo.application.impl, consulo.component.impl;

  // FIXME this export not work as expected in multiclassloader system, but need for compilation, this export hardcoded in Java9ModuleInitializer
  exports consulo.container.impl.securityManager.impl to consulo.application.impl;

  uses consulo.container.boot.ContainerStartup;

  provides consulo.container.plugin.internal.PluginManagerInternal with consulo.container.impl.PluginManagerInternalImpl;
}
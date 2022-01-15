module consulo.container.impl {
  requires consulo.container.api;
  requires consulo.util.nodep;

  // add temp dependency
  requires transitive java.scripting;
  // this depedency fix batik runtime
  requires transitive jdk.xml.dom;

  exports consulo.container.impl;
  exports consulo.container.impl.classloader;
  // TODO [VISTALL] must be exported only to advanced proxy module
  exports consulo.container.impl.classloader.proxy;
  exports consulo.container.impl.parser;
  // TODO [VISTALL] export only to core module
  exports consulo.container.impl.securityManager.impl;

  uses consulo.container.boot.ContainerStartup;

  provides consulo.container.plugin.internal.PluginManagerInternal with consulo.container.impl.PluginManagerInternalImpl;
}
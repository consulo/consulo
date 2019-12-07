module consulo.container.api {
  requires consulo.util.nodep;

  exports consulo.container;
  exports consulo.container.boot;
  exports consulo.container.classloader;
  exports consulo.container.plugin;
  exports consulo.container.plugin.util;
  exports consulo.container.util;

  exports consulo.container.plugin.internal to consulo.container.impl;
  exports consulo.container.boot.internal to consulo.container.impl;

  uses consulo.container.boot.ContainerStartup;
  uses consulo.container.plugin.internal.PluginManagerInternal;
}
module consulo.container.api {
  requires consulo.util.nodep;

  exports consulo.container;
  exports consulo.container.boot;
  exports consulo.container.classloader;
  exports consulo.container.plugin;
  exports consulo.container.plugin.util;
  exports consulo.container.util;

  exports consulo.container.internal to consulo.container.impl, consulo.application.impl, consulo.desktop.awt.bootstrap, consulo.desktop.swt.bootstrap, consulo.desktop.swt.ide.impl;

  uses consulo.container.boot.ContainerStartup;
  uses consulo.container.internal.PluginManagerInternal;
}
module consulo.container.api {
  requires consulo.util.nodep;

  exports consulo.container;
  exports consulo.container.boot;
  exports consulo.container.classloader;
  exports consulo.container.plugin;
  exports consulo.container.plugin.util;
  exports consulo.container.util;
  // TODO [VISTALL] export only to impl module
  exports consulo.container.plugin.internal;

  uses consulo.container.boot.ContainerStartup;
}
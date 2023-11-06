/**
 * @author VISTALL
 * @since 16/01/2022
 */
module consulo.test.impl {
  requires transitive consulo.container.api;
  requires transitive consulo.application.api;
  requires transitive consulo.project.api;
  requires transitive consulo.module.api;
  requires transitive consulo.document.api;
  requires transitive consulo.language.api;
  requires transitive consulo.ui.api;
  requires transitive consulo.ui.ex.api;

  requires consulo.component.impl;
  requires consulo.language.impl;
  requires consulo.document.impl;
  requires consulo.platform.impl;

  exports consulo.test.light;
  exports consulo.test.light.impl;

  provides consulo.platform.internal.PlatformInternal with consulo.test.light.impl.LightPlatformInternal;
  provides consulo.container.internal.PluginManagerInternal with consulo.test.light.impl.LightPluginManager;
}


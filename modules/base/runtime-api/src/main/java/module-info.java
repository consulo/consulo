/**
 * @author VISTALL
 * @since 13/01/2022
 */
module consulo.base.runtime.api {
  requires consulo.ui.api;
  requires consulo.annotation;
  requires consulo.container.api;

  uses consulo.platform.internal.PlatformInternal;
  
  exports consulo.platform;
  //TODO restrict to impl module
  exports consulo.platform.internal;
  //exports consulo.platform.internal to consulo.desktop.awt.ide;
}
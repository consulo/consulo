/**
 * @author VISTALL
 * @since 13/01/2022
 */
module consulo.platform.api {
  requires consulo.ui.api;
  requires consulo.annotation;
  requires consulo.container.api;
  requires consulo.util.lang;
  requires consulo.util.dataholder;

  uses consulo.platform.internal.PlatformInternal;
  
  exports consulo.platform;
  exports consulo.platform.os;
  exports consulo.platform.internal to consulo.desktop.awt.ide.impl, consulo.desktop.swt.ide.impl;
}
/**
 * @author VISTALL
 * @since 14/01/2022
 */
module consulo.platform.impl {
  requires consulo.annotation;
  requires consulo.platform.api;
  requires consulo.util.lang;

  requires com.sun.jna;
  requires com.sun.jna.platform;

  opens consulo.platform.impl to com.sun.jna;

  exports consulo.platform.impl to
    consulo.desktop.ide.impl,
    consulo.desktop.awt.ide.impl,
    consulo.desktop.swt.ide.impl;
}
/**
 * @author VISTALL
 * @since 16/01/2022
 */
module consulo.desktop.ide.impl {
  requires consulo.ide.impl;

  requires com.sun.jna;
  requires com.sun.jna.platform;

  requires consulo.container.api;

  exports consulo.desktop.application.util;
}
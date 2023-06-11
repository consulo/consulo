/**
 * @author VISTALL
 * @since 14/01/2022
 */
module consulo.platform.impl {
  requires consulo.annotation;
  requires consulo.platform.api;
  requires consulo.util.lang;
  requires consulo.util.collection;
  requires consulo.util.dataholder;

  requires com.sun.jna;
  requires com.sun.jna.platform;
  requires org.slf4j;

  opens consulo.platform.impl to com.sun.jna;

  exports consulo.platform.impl to
    consulo.desktop.ide.impl,
    consulo.desktop.awt.ide.impl,
    consulo.desktop.swt.ide.impl;
}
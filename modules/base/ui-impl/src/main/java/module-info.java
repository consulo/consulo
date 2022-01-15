/**
 * @author VISTALL
 * @since 13/01/2022
 */
module consulo.ui.impl {
  requires consulo.annotation;
  requires consulo.base.proxy;
  requires consulo.disposer.api;
  requires consulo.logging.api;
  requires consulo.container.api;
  requires consulo.ui.api;
  requires consulo.localize.api;

  requires consulo.base.localize.library;
  
  requires consulo.util.collection;
  requires consulo.util.dataholder;
  requires consulo.util.lang;
  requires consulo.util.io;

  // exports impl packages only to impl modules
  exports consulo.ui.impl to consulo.base.ide, consulo.desktop.awt.ide;
  exports consulo.ui.impl.image to consulo.base.ide, consulo.desktop.awt.ide;
  exports consulo.ui.impl.model to consulo.base.ide, consulo.desktop.awt.ide;
  exports consulo.ui.impl.style to consulo.base.ide, consulo.desktop.awt.ide;
}
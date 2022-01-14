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

  exports consulo.ui.impl;
  exports consulo.ui.impl.image;
  exports consulo.ui.impl.model;
  exports consulo.ui.impl.style;
}
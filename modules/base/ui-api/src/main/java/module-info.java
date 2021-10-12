import consulo.ui.internal.UIInternal;

/**
 * @author VISTALL
 * @since 2020-10-24
 */
module consulo.ui.api {
  requires consulo.util.lang;
  requires consulo.util.collection;
  requires consulo.util.concurrent;
  requires consulo.util.dataholder;
  requires consulo.localize.api;
  requires consulo.disposer.api;

  requires consulo.container.api;

  exports consulo.ui;
  exports consulo.ui.annotation;
  exports consulo.ui.color;
  exports consulo.ui.cursor;
  exports consulo.ui.border;
  exports consulo.ui.event;
  exports consulo.ui.font;
  exports consulo.ui.image;
  exports consulo.ui.image.canvas;
  exports consulo.ui.layout;
  exports consulo.ui.model;
  exports consulo.ui.style;
  exports consulo.ui.util;

  // TODO [VISTALL] exports only to impl module
  exports consulo.ui.internal;

  uses UIInternal;
}
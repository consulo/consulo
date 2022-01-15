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
  exports consulo.ui.event.details;
  exports consulo.ui.font;
  exports consulo.ui.image;
  exports consulo.ui.image.canvas;
  exports consulo.ui.layout;
  exports consulo.ui.model;
  exports consulo.ui.style;
  exports consulo.ui.util;

  // TODO [VISTALL] this is not supported by classloader? its throw exception
  // Caused by: java.lang.IllegalAccessError: superclass access check failed: class consulo.desktop.awt.ui.impl.DesktopUIInternalImpl (in module consulo.desktop.awt.ide) cannot access class consulo.ui.internal.UIInternal (in module consulo.ui.api) because module consulo.ui.api does not export consulo.ui.internal to module consulo.desktop.awt.ide

  exports consulo.ui.internal;
  exports consulo.ui.image.internal;
  //exports consulo.ui.internal to consulo.desktop.awt.ide;

  uses consulo.ui.image.IconLibraryDescriptor;
  uses consulo.ui.internal.UIInternal;
}
/**
 * @author VISTALL
 * @since 2020-10-19
 */
module consulo.localize.api {
  requires transitive consulo.annotation;
  requires transitive consulo.disposer.api;

  requires consulo.container.api;

  exports consulo.localize;

  uses consulo.localize.LocalizeManager;

  exports consulo.localize.internal to
      consulo.localize.impl,
      consulo.application.impl;
}
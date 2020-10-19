/**
 * @author VISTALL
 * @since 2020-10-19
 */
module consulo.localize.api {
  requires transitive consulo.annotation;
  requires transitive consulo.disposer.api;

  exports consulo.localize;

  uses consulo.localize.LocalizeManager;
}
/**
 * @author VISTALL
 * @since 29/01/2022
 */
module consulo.datacontext.api {
  // todo obsolete dependency
  requires java.desktop;

  requires transitive consulo.application.api;
  requires transitive consulo.util.dataholder;

  exports consulo.dataContext;
  exports consulo.dataContext.internal to consulo.ide.impl;
}
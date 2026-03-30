import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2022-01-29
 */
@NullMarked
module consulo.datacontext.api {
  // todo obsolete dependency
  requires java.desktop;

  requires transitive consulo.component.api;
  requires transitive consulo.util.dataholder;

  exports consulo.dataContext;
  exports consulo.dataContext.internal to
    consulo.execution.impl,
    consulo.ide.impl;
}
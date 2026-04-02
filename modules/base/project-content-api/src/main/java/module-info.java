import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2022-01-21
 */
@NullMarked
module consulo.project.content.api {
  requires transitive consulo.application.content.api;

  exports consulo.project.content;
  exports consulo.project.content.scope;
  exports consulo.project.content.library;
}
import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2022-01-13
 */
@NullMarked
module consulo.base.localize.library {
  requires consulo.localize.api;

  exports consulo.platform.base.localize;
}
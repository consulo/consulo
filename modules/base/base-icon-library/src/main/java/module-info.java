import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2022-01-13
 */
@NullMarked
module consulo.base.icon.library {
  requires consulo.annotation;
  requires consulo.ui.api;

  exports consulo.platform.base.icon;
}
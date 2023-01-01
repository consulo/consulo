package consulo.desktop.awt.ui.impl.image.libraryImage;

import consulo.ui.ex.awt.JBUI;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-10-01
 */
public interface DesktopLibraryInnerImage extends Image {
  @Nonnull
  Image makeGrayed();

  void dropCache();

  @Nonnull
  java.awt.Image toAWTImage(JBUI.ScaleContext ctx);

  @Nonnull
  default Image copyWithScale(float scale) {
    return this;
  }
}

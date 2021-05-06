package consulo.ui.desktop.internal.image.libraryImage;

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
  java.awt.Image toAWTImage();

  @Nonnull
  default Image copyWithScale(float scale) {
    return this;
  }
}

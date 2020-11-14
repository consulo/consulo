package consulo.ui.desktop.internal.image;

import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2020-10-05
 */
public interface DesktopImage<I extends DesktopImage<I>> extends Image {
  @Nonnull
  @SuppressWarnings("unchecked")
  default I copyWithTargetIconLibrary(@Nonnull String iconLibraryId, @Nonnull Function<Image, Image> converter) {
    return (I)this;
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  default I copyWithScale(float scale) {
    return (I)this;
  }
}

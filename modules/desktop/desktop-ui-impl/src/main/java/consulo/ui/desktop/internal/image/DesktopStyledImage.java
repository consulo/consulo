package consulo.ui.desktop.internal.image;

import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2020-10-05
 */
public interface DesktopStyledImage<I extends DesktopStyledImage<I>> extends Image {
  @Nonnull
  I withTargetIconLibrary(@Nonnull String iconLibraryId, @Nonnull Function<Image, Image> converter);
}

package consulo.ui.migration;

import consulo.ui.image.Image;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.net.URL;

/**
 * @author VISTALL
 * @since 18-Jan-17
 */
public interface SwingImageRef extends Icon, Image {
  @NotNull
  URL getIconURL();
}

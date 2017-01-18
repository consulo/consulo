package consulo.ui.migration;

import consulo.ui.ImageRef;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.net.URL;

/**
 * @author VISTALL
 * @since 18-Jan-17
 */
public interface SwingImageRef extends Icon, ImageRef {
  @NotNull
  URL getIconURL();
}

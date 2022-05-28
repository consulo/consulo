package consulo.ui.ex;

import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

/**
 * Base interface for color text container.
 * For AWT/Swing code use {@link consulo.ui.ex.awt.SimpleColoredComponent}
 * If you want just get string from container - use {@link ColoredStringBuilder}
 */
public interface ColoredTextContainer {
  default void append(@Nonnull String fragment) {
    append(fragment, SimpleTextAttributes.REGULAR_ATTRIBUTES);
  }

  void append(@Nonnull String fragment, @Nonnull SimpleTextAttributes attributes);

  void append(@Nonnull String fragment, @Nonnull SimpleTextAttributes attributes, Object tag);

  void setIcon(@Nullable Image image);

  void setToolTipText(@Nullable String text);

  default void setFont(Font font) {
  }
}
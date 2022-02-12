package consulo.ui.ex;

import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ColoredTextContainer {
  void append(@Nonnull String fragment, @Nonnull SimpleTextAttributes attributes);

  void append(@Nonnull String fragment, @Nonnull SimpleTextAttributes attributes, Object tag);

  void setIcon(@Nullable Image image);

  void setToolTipText(@Nullable String text);
}
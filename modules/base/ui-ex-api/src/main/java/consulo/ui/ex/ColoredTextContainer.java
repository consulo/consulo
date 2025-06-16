package consulo.ui.ex;

import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.Iterator;

/**
 * Base interface for color text container.
 * For AWT/Swing code use {@link consulo.ui.ex.awt.SimpleColoredComponent}
 * If you want just get string from container - use {@link ColoredStringBuilder}
 */
public interface ColoredTextContainer {
    default void append(@Nonnull LocalizeValue fragment) {
        append(fragment, SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    default void append(@Nonnull LocalizeValue fragment, @Nonnull SimpleTextAttributes attributes) {
        append(fragment.get(), attributes);
    }

    default void append(@Nonnull LocalizeValue fragment, @Nonnull SimpleTextAttributes attributes, Object tag) {
        append(fragment.get(), attributes, tag);
    }

    default void append(@Nonnull String fragment) {
        append(fragment, SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    void append(@Nonnull String fragment, @Nonnull SimpleTextAttributes attributes);

    void append(@Nonnull String fragment, @Nonnull SimpleTextAttributes attributes, Object tag);

    void setIcon(@Nullable Image image);

    void setToolTipText(@Nullable String text);

    default void setFont(Font font) {
    }

    default void setBackground(Color background) {
    }

    default void clear() {
    }

    @Nonnull
    CharSequence getCharSequence(boolean mainOnly);

    @Nonnull
    ColoredIterator iterator();

    public interface ColoredIterator extends Iterator<String> {
        int getOffset();

        int getEndOffset();

        @Nonnull
        String getFragment();

        @Nonnull
        SimpleTextAttributes getTextAttributes();

        int split(int offset, @Nonnull SimpleTextAttributes attributes);
    }

}
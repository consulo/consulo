package consulo.ui.ex;

import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.Iterator;

/**
 * Base interface for color text container.
 * For AWT/Swing code use {@link consulo.ui.ex.awt.SimpleColoredComponent}
 * If you want just get string from container - use {@link ColoredStringBuilder}
 */
public interface ColoredTextContainer {
    default void append(LocalizeValue fragment) {
        append(fragment, SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    default void append(LocalizeValue fragment, SimpleTextAttributes attributes) {
        append(fragment.get(), attributes);
    }

    default void append(LocalizeValue fragment, SimpleTextAttributes attributes, Object tag) {
        append(fragment.get(), attributes, tag);
    }

    default void append(String fragment) {
        append(fragment, SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    void append(String fragment, SimpleTextAttributes attributes);

    void append(String fragment, SimpleTextAttributes attributes, Object tag);

    void setIcon(@Nullable Image image);

    void setToolTipText(@Nullable String text);

    default void setFont(Font font) {
    }

    default void setBackground(Color background) {
    }

    default void clear() {
    }

    
    CharSequence getCharSequence(boolean mainOnly);

    
    ColoredIterator iterator();

    public interface ColoredIterator extends Iterator<String> {
        int getOffset();

        int getEndOffset();

        
        String getFragment();

        
        SimpleTextAttributes getTextAttributes();

        int split(int offset, SimpleTextAttributes attributes);
    }

}
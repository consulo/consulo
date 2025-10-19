package consulo.execution.debug.frame;

import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

public interface XValueCallback {
    /**
     * Indicate that an error occurs
     *
     * @param errorMessage message describing the error
     */
    @Deprecated
    default void errorOccurred(@Nonnull String errorMessage) {
        errorOccurred(LocalizeValue.of(errorMessage));
    }

    /**
     * Indicate that an error occurs
     *
     * @param errorMessage message describing the error
     */
    void errorOccurred(@Nonnull LocalizeValue errorMessage);
}
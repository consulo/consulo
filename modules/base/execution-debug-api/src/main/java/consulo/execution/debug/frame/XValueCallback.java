package consulo.execution.debug.frame;

import consulo.localize.LocalizeValue;

public interface XValueCallback {
    /**
     * Indicate that an error occurs
     *
     * @param errorMessage message describing the error
     */
    @Deprecated
    default void errorOccurred(String errorMessage) {
        errorOccurred(LocalizeValue.of(errorMessage));
    }

    /**
     * Indicate that an error occurs
     *
     * @param errorMessage message describing the error
     */
    void errorOccurred(LocalizeValue errorMessage);
}
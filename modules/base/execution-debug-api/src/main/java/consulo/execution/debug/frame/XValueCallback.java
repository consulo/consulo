package consulo.execution.debug.frame;

import jakarta.annotation.Nonnull;

public interface XValueCallback {
  /**
   * Indicate that an error occurs
   * @param errorMessage message describing the error
   */
  void errorOccurred(@Nonnull String errorMessage);
}
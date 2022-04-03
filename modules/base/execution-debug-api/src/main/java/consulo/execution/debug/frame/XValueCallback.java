package consulo.execution.debug.frame;

import javax.annotation.Nonnull;

public interface XValueCallback {
  /**
   * Indicate that an error occurs
   * @param errorMessage message describing the error
   */
  void errorOccurred(@Nonnull String errorMessage);
}
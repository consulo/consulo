package consulo.execution.test.sm.ui;

import consulo.process.ProcessHandler;
import javax.annotation.Nonnull;

/**
 * @author Sergey Simonchik
 */
public interface AttachToProcessListener {
  void onAttachToProcess(@Nonnull ProcessHandler processHandler);
}

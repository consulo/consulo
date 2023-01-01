package consulo.ide.impl.idea.execution;

import consulo.process.ExecutionException;

public class ExecutionFinishedException extends ExecutionException {
  public ExecutionFinishedException() {
    this(null);
  }

  public ExecutionFinishedException(Throwable cause) {
    super("Execution finished" + (cause == null || cause.getMessage() == null ? "" : " because of: " + cause.getMessage()), cause);
  }
}

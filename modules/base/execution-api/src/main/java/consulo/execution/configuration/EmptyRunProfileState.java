package consulo.execution.configuration;

import consulo.process.ExecutionException;
import consulo.execution.ExecutionResult;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ProgramRunner;
import org.jspecify.annotations.Nullable;

public final class EmptyRunProfileState implements RunProfileState {
  public static RunProfileState INSTANCE = new EmptyRunProfileState();

  private EmptyRunProfileState() {
  }

  @Override
  public @Nullable ExecutionResult execute(Executor executor, ProgramRunner runner) throws ExecutionException {
    return null;
  }
}
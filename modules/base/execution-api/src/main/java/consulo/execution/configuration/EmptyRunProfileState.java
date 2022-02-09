package consulo.execution.configuration;

import consulo.process.ExecutionException;
import consulo.execution.ExecutionResult;
import consulo.execution.Executor;
import consulo.execution.runner.ProgramRunner;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class EmptyRunProfileState implements RunProfileState {
  public static RunProfileState INSTANCE = new EmptyRunProfileState();

  private EmptyRunProfileState() {
  }

  @Nullable
  @Override
  public ExecutionResult execute(Executor executor, @Nonnull ProgramRunner runner) throws ExecutionException {
    return null;
  }
}
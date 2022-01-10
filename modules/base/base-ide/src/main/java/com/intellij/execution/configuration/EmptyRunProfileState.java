package com.intellij.execution.configuration;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ProgramRunner;
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
// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.execution;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.icons.AllIcons;
import consulo.ui.image.Image;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RunAnythingRunProfile implements RunProfile {
  @Nonnull
  private final String myOriginalCommand;
  @Nonnull
  private final GeneralCommandLine myCommandLine;

  public RunAnythingRunProfile(@Nonnull GeneralCommandLine commandLine, @Nonnull String originalCommand) {
    myCommandLine = commandLine;
    myOriginalCommand = originalCommand;
  }

  @Nullable
  @Override
  public RunProfileState getState(@Nonnull Executor executor, @Nonnull ExecutionEnvironment environment) {
    return new RunAnythingRunProfileState(environment, myOriginalCommand);
  }

  @Nonnull
  @Override
  public String getName() {
    return myOriginalCommand;
  }

  @Nonnull
  public String getOriginalCommand() {
    return myOriginalCommand;
  }

  @Nonnull
  public GeneralCommandLine getCommandLine() {
    return myCommandLine;
  }

  @Nullable
  @Override
  public Image getIcon() {
    return AllIcons.Actions.Run_anything;
  }
}
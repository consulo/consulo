// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.handlers;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Extension;
import consulo.component.extension.ExtensionPointName;
import consulo.execution.ui.console.TextConsoleBuilder;
import consulo.ide.impl.idea.execution.process.KillableProcessHandler;
import consulo.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class customizes 'run anything' command execution settings depending on input command
 */
@Extension(ComponentScope.APPLICATION)
public abstract class RunAnythingCommandHandler {
  public static final ExtensionPointName<RunAnythingCommandHandler> EP_NAME = ExtensionPointName.create(RunAnythingCommandHandler.class);

  public abstract boolean isMatched(@Nonnull String commandLine);

  /**
   * See {@link KillableProcessHandler#shouldKillProcessSoftly()} for details.
   */
  public boolean shouldKillProcessSoftly() {
    return true;
  }

  /**
   * Provides custom output to be printed in console on the process terminated.
   * E.g. command execution time could be reported on a command execution terminating.
   */
  @Nullable
  public String getProcessTerminatedCustomOutput() {
    return null;
  }

  /**
   * Creates console builder for matched command
   */
  public abstract TextConsoleBuilder getConsoleBuilder(@Nonnull Project project);

  @Nullable
  public static RunAnythingCommandHandler getMatchedHandler(@Nonnull String commandLine) {
    return EP_NAME.getExtensionList().stream().filter(handler -> handler.isMatched(commandLine)).findFirst().orElse(null);
  }
}
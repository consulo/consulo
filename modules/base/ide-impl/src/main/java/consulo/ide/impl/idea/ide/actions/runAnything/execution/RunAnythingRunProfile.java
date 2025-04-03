// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.execution;

import consulo.execution.executor.Executor;
import consulo.process.cmd.GeneralCommandLine;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.application.AllIcons;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
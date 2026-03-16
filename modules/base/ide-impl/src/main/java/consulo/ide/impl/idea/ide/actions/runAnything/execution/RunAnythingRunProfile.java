// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.execution;

import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.cmd.GeneralCommandLine;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

public class RunAnythingRunProfile implements RunProfile {
    
    private final String myOriginalCommand;
    
    private final GeneralCommandLine myCommandLine;

    public RunAnythingRunProfile(GeneralCommandLine commandLine, String originalCommand) {
        myCommandLine = commandLine;
        myOriginalCommand = originalCommand;
    }

    @Nullable
    @Override
    public RunProfileState getState(Executor executor, ExecutionEnvironment environment) {
        return new RunAnythingRunProfileState(environment, myOriginalCommand);
    }

    
    @Override
    public String getName() {
        return myOriginalCommand;
    }

    
    public String getOriginalCommand() {
        return myOriginalCommand;
    }

    
    public GeneralCommandLine getCommandLine() {
        return myCommandLine;
    }

    @Nullable
    @Override
    public Image getIcon() {
        return PlatformIconGroup.actionsRun_anything();
    }
}
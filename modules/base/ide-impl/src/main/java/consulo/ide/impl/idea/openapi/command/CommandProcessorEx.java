// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.command;

import consulo.annotation.DeprecationInfo;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.internal.builder.BaseExecutableCommandBuilder;
import consulo.undoRedo.internal.builder.WrappableRunnableCommandBuilder;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.undoRedo.CommandDescriptor;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.UndoConfirmationPolicy;
import consulo.undoRedo.builder.RunnableCommandBuilder;
import consulo.util.lang.EmptyRunnable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 */
public abstract class CommandProcessorEx extends CommandProcessor {
    public interface StartableCommandBuilder<R, THIS extends StartableCommandBuilder<R, THIS>> extends RunnableCommandBuilder<R, THIS> {
        @RequiredUIAccess
        CommandToken start();
    }

    public abstract void enterModal();

    public abstract void leaveModal();

    @Nonnull
    @Override
    public abstract <T> StartableCommandBuilder<T, ? extends StartableCommandBuilder<T, ?>> newCommand();

    @Nullable
    @RequiredUIAccess
    protected abstract CommandToken startCommand(CommandDescriptor commandDescriptor);

    @Deprecated
    @DeprecationInfo("Use #newCommand().start()")
    @Nullable
    @RequiredUIAccess
    public CommandToken startCommand(
        @Nullable Project project,
        String name,
        @Nullable Object groupId,
        @Nonnull UndoConfirmationPolicy undoConfirmationPolicy
    ) {
        return newCommand()
            .project(project)
            .name(LocalizeValue.ofNullable(name))
            .groupId(groupId)
            .undoConfirmationPolicy(undoConfirmationPolicy)
            .start();
    }
}

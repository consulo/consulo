// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.undoRedo.internal;

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandDescriptor;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.UndoConfirmationPolicy;
import consulo.undoRedo.builder.RunnableCommandBuilder;
import org.jspecify.annotations.Nullable;

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

    
    @Override
    public abstract <T> StartableCommandBuilder<T, ? extends StartableCommandBuilder<T, ?>> newCommand();

    @RequiredUIAccess
    protected abstract @Nullable CommandToken startCommand(CommandDescriptor commandDescriptor);

    @Deprecated
    @DeprecationInfo("Use #newCommand().start()")
    @RequiredUIAccess
    public @Nullable CommandToken startCommand(
        @Nullable Project project,
        String name,
        @Nullable Object groupId,
        UndoConfirmationPolicy undoConfirmationPolicy
    ) {
        StartableCommandBuilder<?, ?> builder = newCommand();
        if (project != null) {
            builder = builder.project(project);
        }
        if (groupId != null) {
            builder = builder.groupId(groupId);
        }
        return builder
            .name(LocalizeValue.ofNullable(name))
            .undoConfirmationPolicy(undoConfirmationPolicy)
            .start();
    }
}

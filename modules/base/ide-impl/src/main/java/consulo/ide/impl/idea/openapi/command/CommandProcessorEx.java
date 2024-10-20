// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.command;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.undoRedo.CommandDescriptor;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.UndoConfirmationPolicy;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 */
public abstract class CommandProcessorEx extends CommandProcessor {
    public abstract void enterModal();

    public abstract void leaveModal();

    @Nullable
    public abstract CommandToken startCommand(CommandDescriptor commandDescriptor);

    @Deprecated(forRemoval = true)
    @Nullable
    public CommandToken startCommand(
        @Nullable Project project,
        String name,
        @Nullable Object groupId,
        @Nonnull UndoConfirmationPolicy undoConfirmationPolicy
    ) {
        return startCommand(
            new CommandDescriptor()
                .project(project)
                .name(LocalizeValue.ofNullable(name))
                .groupId(groupId)
                .undoConfirmationPolicy(undoConfirmationPolicy)
        );
    }

    public abstract void finishCommand(@Nonnull final CommandToken command, @Nullable Throwable throwable);
}

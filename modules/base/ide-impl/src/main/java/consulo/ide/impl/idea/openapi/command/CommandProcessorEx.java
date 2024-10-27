// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.command;

import consulo.ide.impl.idea.openapi.command.impl.BaseCommandBuilder;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.undoRedo.CommandDescriptor;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.UndoConfirmationPolicy;
import consulo.undoRedo.builder.CommandBuilder;
import consulo.util.lang.EmptyRunnable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 */
public abstract class CommandProcessorEx extends CommandProcessor {
    public interface StartableCommandBuilder extends CommandBuilder<StartableCommandBuilder> {
        CommandToken start();
    }

    protected class MyStartableCommandBuilder extends BaseCommandBuilder<StartableCommandBuilder> implements StartableCommandBuilder {
        @Override
        public CommandToken start() {
            return startCommand(build(EmptyRunnable.INSTANCE));
        }
    }

    public abstract void enterModal();

    public abstract void leaveModal();

    @Nonnull
    public StartableCommandBuilder newCommand() {
        return new MyStartableCommandBuilder();
    }

    @Nullable
    protected abstract CommandToken startCommand(CommandDescriptor commandDescriptor);

    @Deprecated(forRemoval = true)
    @Nullable
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

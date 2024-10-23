// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.command;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.undoRedo.*;
import consulo.ide.impl.idea.openapi.command.impl.BaseCommandBuilder;
import consulo.undoRedo.builder.CommandBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 */
public abstract class CommandProcessorEx extends CommandProcessor {
    public interface StartableCommandBuilder extends CommandBuilder<StartableCommandBuilder> {
        CommandToken start();
    }

    public class MyStartableCommandBuilder extends BaseCommandBuilder<StartableCommandBuilder> implements StartableCommandBuilder {
        @Override
        public CommandToken start() {
            return startCommand(build());
        }
    }

    public abstract void enterModal();

    public abstract void leaveModal();

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
            .withProject(project)
            .withName(LocalizeValue.ofNullable(name))
            .withGroupId(groupId)
            .withUndoConfirmationPolicy(undoConfirmationPolicy)
            .start();
    }
}

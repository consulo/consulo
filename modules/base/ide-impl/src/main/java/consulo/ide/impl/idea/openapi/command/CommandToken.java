// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.command;

import consulo.project.Project;
import consulo.undoRedo.CommandDescriptor;

import jakarta.annotation.Nullable;

/**
 * Represents a command currently being executed by a command processor.
 *
 * @author yole
 * @see CommandProcessorEx#startCommand(CommandDescriptor)
 */
public interface CommandToken {
    @Nullable
    Project getProject();
}

// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.undoRedo.internal;

import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.UndoConfirmationPolicy;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

/**
 * @author max
 */
public interface CommandProcessorEx extends CommandProcessor {
  void enterModal();

  void leaveModal();

  @Nullable
  CommandToken startCommand(@Nullable Project project,
                            @Nls String name,
                            @Nullable Object groupId,
                            @Nonnull UndoConfirmationPolicy undoConfirmationPolicy);

  void finishCommand(@Nonnull final CommandToken command, @Nullable Throwable throwable);
}

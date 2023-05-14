// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.event;

import consulo.execution.ui.ExecutionConsole;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

//@ApiStatus.Experimental
public interface BuildEventPresentationData {
  @Nonnull
  Image getNodeIcon();

  @Nullable ExecutionConsole getExecutionConsole();

  @Nullable
  ActionGroup consoleToolbarActions();
}

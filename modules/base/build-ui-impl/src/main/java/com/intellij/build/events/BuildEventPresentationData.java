// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.events;

import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.actionSystem.ActionGroup;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

//@ApiStatus.Experimental
public interface BuildEventPresentationData {
  @Nonnull
  Image getNodeIcon();

  @Nullable ExecutionConsole getExecutionConsole();

  @Nullable
  ActionGroup consoleToolbarActions();
}

// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.items;

import com.intellij.openapi.actionSystem.AnAction;
import consulo.ui.image.Image;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RunAnythingActionItem<T extends AnAction> extends RunAnythingItemBase {
  @Nonnull
  private final T myAction;

  public RunAnythingActionItem(@Nonnull T action, @Nonnull String fullCommand, @Nullable Image icon) {
    super(fullCommand, icon);
    myAction = action;
  }

  @Nonnull
  public static String getCommand(@Nonnull AnAction action, @Nonnull String command) {
    return command + " " + (action.getTemplatePresentation().getText() != null ? action.getTemplatePresentation().getText() : "undefined");
  }

  @Nullable
  @Override
  public String getDescription() {
    return myAction.getTemplatePresentation().getDescription();
  }
}
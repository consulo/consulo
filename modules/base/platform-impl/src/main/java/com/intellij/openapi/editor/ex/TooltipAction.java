// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.ex;

import com.intellij.openapi.editor.Editor;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.event.InputEvent;

/**
 * High level info for showing the action as a part of the error tooltip
 */
public interface TooltipAction {

  @Nonnull
  String getText();

  void execute(@Nonnull Editor editor, @Nullable InputEvent event);

  void showAllActions(@Nonnull Editor editor);
}
// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.impl.internal.hint;

import consulo.codeEditor.Editor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
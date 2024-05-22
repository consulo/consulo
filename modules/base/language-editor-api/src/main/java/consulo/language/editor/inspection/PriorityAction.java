// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.inspection;

import consulo.language.editor.intention.IntentionAction;

import jakarta.annotation.Nonnull;

/**
 * Interface for {@link IntentionAction intentions} and {@link LocalQuickFix quick fixes}.
 */
public interface PriorityAction {

  enum Priority {
    TOP,
    HIGH,
    NORMAL,
    LOW,
    BOTTOM
  }

  @Nonnull
  Priority getPriority();
}

// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.reactions;

import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.List;

public interface CodeReviewReactionPillPresentation {
  @Nls @Nonnull String getReactionName();

  @Nonnull List<@Nls String> getReactors();

  boolean isOwnReaction();

  @Nonnull Icon getIcon(int size);
}

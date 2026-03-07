// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.editor;

import kotlinx.coroutines.flow.StateFlow;
import jakarta.annotation.Nullable;

import java.util.Collection;

/**
 * A UI model for an editor with code review inlays
 * This model should exist in the same scope as the gutter
 * One model - one gutter
 */
public interface CodeReviewEditorInlaysModel<I extends CodeReviewInlayModel> {
    StateFlow<Collection<I>> getInlays();
}

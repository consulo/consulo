// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.editor;

import consulo.util.concurrent.coroutine.ObservableValue;

import java.util.Collection;

/**
 * A UI model for an editor with code review inlays
 * <p>
 * This model should exist in the same scope as the gutter
 * <p>
 * One model - one gutter
 */
public interface CodeReviewEditorInlaysModel<I extends CodeReviewInlayModel> {
    ObservableValue<Collection<I>> getInlays();
}

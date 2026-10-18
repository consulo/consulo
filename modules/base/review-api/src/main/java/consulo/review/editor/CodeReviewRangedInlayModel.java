// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.editor;

import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

/**
 * An inlay about several lines, which the editor frames while it is hovered or focused.
 */
public interface CodeReviewRangedInlayModel extends CodeReviewInlayModel {
    ObservableValue<@Nullable CodeReviewLineRange> getRange();
}

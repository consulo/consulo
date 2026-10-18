// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.editor;

/**
 * Why the lines of an adjustable inlay cannot be changed.
 */
public enum CodeReviewInlayAdjustmentDisabledReason {
    SUGGESTED_CHANGE,
    SINGLE_COMMIT_REVIEW,
    UNSUPPORTED_VERSION
}

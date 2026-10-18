// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.toolwindow;

import consulo.util.dataholder.Key;

public final class ReviewToolwindowDataKeys {
    public static final Key<ReviewToolwindowProjectViewModel<?, ?>> REVIEW_TOOLWINDOW_PROJECT_VM =
        Key.create("consulo.review.toolwindow.review.project.vm");

    public static final Key<ReviewToolwindowViewModel<?>> REVIEW_TOOLWINDOW_VM = Key.create("consulo.review.toolwindow.review.toolwindow.vm");

    private ReviewToolwindowDataKeys() {
    }
}

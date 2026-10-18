// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.comment;

import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.review.util.ComputedResult;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

public interface CodeReviewSubmittableTextViewModel {
    Project getProject();

    /**
     * Input text state
     */
    MutableObservableValue<String> getText();

    /**
     * State of submission progress
     * <p>
     * null means that submission wasn't started yet
     */
    ObservableValue<@Nullable ComputedResult<?>> getState();

    void addFocusRequestListener(Disposable parentDisposable, Runnable listener);

    void requestFocus();
}

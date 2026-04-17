// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.collaboration.ui.codereview.comment;

import consulo.collaboration.util.ComputedResult;
import consulo.collaboration.util.ObservableValue;
import consulo.collaboration.util.ReadOnlyObservableValue;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

/**
 * View model for a submittable text input (e.g. comment, review body).
 */
public interface CodeReviewSubmittableTextViewModel {
    Project getProject();

    /**
     * Input text state.
     */
    ObservableValue<String> getText();

    /**
     * State of submission progress.
     * null means that submission wasn't started yet.
     */
    ReadOnlyObservableValue<@Nullable ComputedResult<Void>> getState();

    void requestFocus();
}

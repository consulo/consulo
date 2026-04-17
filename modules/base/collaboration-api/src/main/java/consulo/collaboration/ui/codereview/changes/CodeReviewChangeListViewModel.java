// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.collaboration.ui.codereview.changes;

import consulo.collaboration.util.ReadOnlyObservableValue;
import consulo.project.Project;

import java.util.List;

/**
 * View model for the list of changed files in a code review.
 */
public interface CodeReviewChangeListViewModel {
    Project getProject();

    ReadOnlyObservableValue<List<String>> getChangedFiles();

    void showDiff();
}

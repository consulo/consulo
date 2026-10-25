// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.diff.model;

import consulo.diff.request.DiffRequest;
import consulo.review.util.ComputedResult;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

/**
 * A viewmodel for a diff between two changes
 * Implementations should provide an equals/hashCode implementation
 */
public interface AsyncDiffViewModel {
    ObservableValue<@Nullable ComputedResult<DiffRequest>> getRequest();

    void reloadRequest();
}

// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.diff.model;

import consulo.disposer.Disposable;

/**
 * @param <D>   preloaded data type
 * @param <C>   change type
 * @param <CVM> change view model type
 */
@FunctionalInterface
public interface CodeReviewDiffViewModelFactory<D, C, CVM extends AsyncDiffViewModel> {
    CVM create(Disposable scope, D preloadedData, C change);
}

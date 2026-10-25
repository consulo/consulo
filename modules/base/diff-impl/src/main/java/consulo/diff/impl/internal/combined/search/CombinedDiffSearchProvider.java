// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined.search;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.diff.impl.internal.combined.CombinedDiffViewer;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * Provides "search in multiple editors" functionality for {@link CombinedDiffViewer}
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface CombinedDiffSearchProvider {
    @RequiredUIAccess
    void installSearch(CombinedDiffViewer viewer);
}

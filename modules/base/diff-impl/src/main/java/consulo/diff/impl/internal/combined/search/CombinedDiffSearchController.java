// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined.search;

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * Controller to update search in particular {@link consulo.diff.impl.internal.combined.CombinedDiffMainUI}
 */
public interface CombinedDiffSearchController {
    Component getSearchComponent();

    @RequiredUIAccess
    void update(CombinedDiffSearchContext context);
}

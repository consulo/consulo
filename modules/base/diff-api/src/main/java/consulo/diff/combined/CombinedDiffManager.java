// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.combined;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import org.jspecify.annotations.Nullable;

import java.util.List;

@ServiceAPI(ComponentScope.PROJECT)
public interface CombinedDiffManager {
    static CombinedDiffManager getInstance(Project project) {
        return project.getInstance(CombinedDiffManager.class);
    }

    @RequiredUIAccess
    default CombinedDiffComponentProcessor createProcessor() {
        return createProcessor(null, null, null);
    }

    @RequiredUIAccess
    default CombinedDiffComponentProcessor createProcessor(@Nullable String diffPlace) {
        return createProcessor(diffPlace, null, null);
    }

    @RequiredUIAccess
    CombinedDiffComponentProcessor createProcessor(
        @Nullable String diffPlace,
        @Nullable List<AnAction> contextActions,
        @Nullable List<AnAction> goToChangeToolbarActions
    );
}

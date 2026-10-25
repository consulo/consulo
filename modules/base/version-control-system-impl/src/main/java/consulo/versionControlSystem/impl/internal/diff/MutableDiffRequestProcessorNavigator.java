// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.diff;

import consulo.application.util.ListSelection;
import consulo.versionControlSystem.change.PresentableChange;
import org.jspecify.annotations.Nullable;

public interface MutableDiffRequestProcessorNavigator<C> {
    ListSelection<C> getCurrentList();

    void selectNext(boolean fromDifferences);

    void selectPrev(boolean fromDifferences);

    void select(C change);

    @Nullable PresentableChange getChangePresentation(C change);

    static <C> MutableDiffRequestProcessorNavigator<C> empty() {
        return new MutableDiffRequestProcessorNavigator<>() {
            @Override
            public ListSelection<C> getCurrentList() {
                return ListSelection.empty();
            }

            @Override
            public void selectNext(boolean fromDifferences) {
            }

            @Override
            public void selectPrev(boolean fromDifferences) {
            }

            @Override
            public void select(C change) {
            }

            @Override
            public @Nullable PresentableChange getChangePresentation(C change) {
                return null;
            }
        };
    }
}

// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.impl.internal.TextDiffSettingsHolder;
import consulo.diff.impl.internal.viewer.FoldingModelSupport;
import consulo.diff.impl.internal.viewer.TextDiffViewerUtil;

import java.util.List;
import java.util.function.Supplier;

class CombinedToggleExpandByDefaultAction extends TextDiffViewerUtil.ToggleExpandByDefaultAction {
    private final Supplier<List<FoldingModelSupport>> myFoldingModels;

    CombinedToggleExpandByDefaultAction(TextDiffSettingsHolder.TextDiffSettings textSettings, Supplier<List<FoldingModelSupport>> foldingModels) {
        super(textSettings);
        myFoldingModels = foldingModels;
    }

    @Override
    protected void expandAll(boolean expand) {
        for (FoldingModelSupport model : myFoldingModels.get()) {
            model.expandAll(expand);
        }
    }
}

// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Caret;
import consulo.dataContext.DataContext;
import consulo.ui.ex.action.ActionPromoter;
import consulo.ui.ex.action.AnAction;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@ExtensionImpl
public class CombinedDiffActionPromoter implements ActionPromoter {
    @Override
    public @Nullable List<AnAction> suppress(List<AnAction> actions, DataContext context) {
        if (context.getData(CombinedDiffViewerKeys.COMBINED_DIFF_VIEWER) == null) {
            return null;
        }

        Caret caret = context.getData(Caret.KEY);
        if (caret != null) {
            List<AnAction> suppressed = new ArrayList<>();
            for (AnAction action : actions) {
                if (action instanceof CombinedGlobalBlockNavigationAction) {
                    suppressed.add(action);
                }
            }
            return suppressed;
        }

        return null;
    }
}

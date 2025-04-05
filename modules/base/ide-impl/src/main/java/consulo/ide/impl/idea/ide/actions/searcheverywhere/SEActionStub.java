// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;

public class SEActionStub extends DumbAwareAction {
    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
    }

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabled(false);
    }
}

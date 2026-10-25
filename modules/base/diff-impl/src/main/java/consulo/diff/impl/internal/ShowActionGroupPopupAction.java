// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal;

import consulo.ui.ex.action.DumbAwareAction;
import consulo.diff.localize.DiffLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.EmptyAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public class ShowActionGroupPopupAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    private final Supplier<@Nullable ActionGroup> myPopupActionGroup;

    public ShowActionGroupPopupAction(Supplier<@Nullable ActionGroup> popupActionGroup) {
        myPopupActionGroup = popupActionGroup;
        EmptyAction.setupAction(this, "Diff.ShowSettingsPopup", null);
    }

    @Override
    public void update(AnActionEvent e) {
        ActionGroup group = myPopupActionGroup.get();
        e.getPresentation().setEnabled(group != null && group.getChildren(null).length > 0);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        ActionGroup group = myPopupActionGroup.get();
        if (group == null) {
            return;
        }
        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
            DiffLocalize.diffActions().get(),
            group,
            e.getDataContext(),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            false
        );
        popup.showInBestPositionFor(e.getDataContext());
    }
}

// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.diff;

import consulo.application.util.ListSelection;
import consulo.diff.internal.GoToChangePopupAction;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;
import consulo.versionControlSystem.change.PresentableChange;
import org.jspecify.annotations.Nullable;

public final class PresentableGoToChangePopupAction<C> extends GoToChangePopupAction {
    private final MutableDiffRequestProcessorNavigator<C> myNavigator;

    public PresentableGoToChangePopupAction(MutableDiffRequestProcessorNavigator<C> navigator) {
        myNavigator = navigator;
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(myNavigator.getCurrentList().getList().size() > 1);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        ListSelection<C> selection = myNavigator.getCurrentList();
        BaseListPopupStep<C> step = new BaseListPopupStep<>(null, selection.getList()) {
            @Override
            public String getTextFor(C value) {
                PresentableChange presentation = myNavigator.getChangePresentation(value);
                return presentation == null ? String.valueOf(value) : presentation.getFilePath().getName();
            }

            @Override
            public @Nullable Image getIconFor(C value) {
                PresentableChange presentation = myNavigator.getChangePresentation(value);
                return presentation == null ? null : presentation.getFilePath().getFileType().getIcon();
            }

            @Override
            public boolean isSpeedSearchEnabled() {
                return true;
            }

            @Override
            public @Nullable PopupStep onChosen(C selectedValue, boolean finalChoice) {
                return doFinalStep(() -> myNavigator.select(selectedValue));
            }
        };
        step.setDefaultOptionIndex(selection.getSelectedIndex());

        JBPopup popup = JBPopupFactory.getInstance().createListPopup(e.getData(Project.KEY), step);
        if (e.getUIEvent() != null) {
            popup.showUnderneathOf(e);
        }
        else {
            popup.showInBestPositionFor(e.getDataContext());
        }
    }
}

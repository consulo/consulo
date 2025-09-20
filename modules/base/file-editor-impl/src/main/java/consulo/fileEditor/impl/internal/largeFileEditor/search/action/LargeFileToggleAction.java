// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.impl.internal.largeFileEditor.search.action;

import consulo.application.dumb.DumbAware;
import consulo.fileEditor.internal.largeFileEditor.LfeSearchManager;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.action.CheckboxAction;
import jakarta.annotation.Nonnull;

public class LargeFileToggleAction extends CheckboxAction implements DumbAware {
    private final LfeSearchManager searchManager;

    private boolean isSelected;

    public LargeFileToggleAction(LfeSearchManager searchManager, LocalizeValue actionText) {
        super(actionText);
        this.searchManager = searchManager;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        if (isSelected != selected) {
            isSelected = selected;
            searchManager.onSearchParametersChanged();
        }
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
        return isSelected;
    }

    @Override
    public @Nonnull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        setSelected(state);
    }
}
// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.searchEverywhere;

import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CheckboxAction;
import jakarta.annotation.Nonnull;

public abstract class CheckBoxSearchEverywhereToggleAction extends CheckboxAction implements DumbAware, SearchEverywhereToggleAction {
    public CheckBoxSearchEverywhereToggleAction(@Nonnull LocalizeValue text) {
        super(text);
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
        return isEverywhere();
    }

    @RequiredUIAccess
    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        setEverywhere(state);
    }

    @Override
    public boolean canToggleEverywhere() {
        return true;
    }
}

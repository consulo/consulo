// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.searchEverywhere;

import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CheckboxAction;
import consulo.ui.ex.action.Presentation;

public abstract class CheckBoxSearchEverywhereToggleAction extends CheckboxAction implements DumbAware, SearchEverywhereToggleAction {
    public CheckBoxSearchEverywhereToggleAction(LocalizeValue text) {
        super(text);
    }

    @RequiredUIAccess
    @Override
    public CheckBox createCustomComponent(Presentation presentation, String place) {
        CheckBox box = super.createCustomComponent(presentation, place);
        box.addBorder(BorderPosition.TOP, BorderStyle.EMPTY, 2);
        return box;
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        return isEverywhere();
    }

    @RequiredUIAccess
    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        setEverywhere(state);
    }

    @Override
    public boolean canToggleEverywhere() {
        return true;
    }
}

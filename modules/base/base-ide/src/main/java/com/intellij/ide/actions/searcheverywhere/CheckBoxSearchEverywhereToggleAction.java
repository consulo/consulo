// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ex.CheckboxAction;
import com.intellij.openapi.project.DumbAware;
import javax.annotation.Nonnull;

public abstract class CheckBoxSearchEverywhereToggleAction extends CheckboxAction implements DumbAware, SearchEverywhereToggleAction {
  public CheckBoxSearchEverywhereToggleAction(@Nonnull String text) {
    super(text);
  }

  @Override
  public boolean isSelected(@Nonnull AnActionEvent e) {
    return isEverywhere();
  }

  @Override
  public void setSelected(@Nonnull AnActionEvent e, boolean state) {
    setEverywhere(state);
  }

  @Override
  public boolean canToggleEverywhere() {
    return true;
  }
}

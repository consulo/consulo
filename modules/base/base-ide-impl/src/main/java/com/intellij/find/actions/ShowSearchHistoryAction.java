// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import javax.annotation.Nonnull;

public class ShowSearchHistoryAction extends AnAction {
  public ShowSearchHistoryAction() {
    super("Search History", "Show search history popup", AllIcons.Actions.SearchWithHistory);
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    //do nothing, it's just shortcut-holding action
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabled(false);
  }
}

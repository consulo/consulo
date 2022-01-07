// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.xdebugger.impl.frame.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.xdebugger.impl.ui.XDebugSessionTab;

import javax.annotation.Nonnull;

public class XSwitchWatchesInVariables extends ToggleAction {
  @Override
  public boolean isSelected(@Nonnull AnActionEvent e) {
    XDebugSessionTab tab = e.getData(XDebugSessionTab.TAB_KEY);
    return tab == null || tab.isWatchesInVariables();
  }

  @Override
  public void setSelected(@Nonnull AnActionEvent e, boolean state) {
    XDebugSessionTab tab = e.getData(XDebugSessionTab.TAB_KEY);
    if (tab != null) {
      tab.setWatchesInVariables(!tab.isWatchesInVariables());
    }
  }
}

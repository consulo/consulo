/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.diff.impl.dir.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.diff.DirDiffSettings;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diff.impl.dir.DirDiffTableModel;
import com.intellij.util.ui.EmptyIcon;

import javax.swing.*;

/**
* @author Konstantin Bulenkov
*/
class ChangeCompareModeAction extends AnAction {
  private final static Icon OFF = EmptyIcon.create(AllIcons.Actions.Checked.getIconHeight());

  private final DirDiffTableModel myModel;
  private final DirDiffSettings.CompareMode myMode;

  ChangeCompareModeAction(DirDiffTableModel model, DirDiffSettings.CompareMode mode) {
    super(mode.getPresentableName(model.getSettings()));
    myModel = model;
    myMode = mode;
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    myModel.setCompareMode(myMode);
    myModel.reloadModel(false);
  }

  @Override
  public void update(AnActionEvent e) {
    final boolean on = myModel.getCompareMode() == myMode;
    e.getPresentation().setIcon(on ? AllIcons.Actions.Checked : OFF);
    e.getPresentation().setSelectedIcon(on ? AllIcons.Actions.Checked_selected : OFF);
  }
}

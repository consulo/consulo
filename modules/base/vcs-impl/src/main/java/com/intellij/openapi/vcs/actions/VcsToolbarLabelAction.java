// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vcs.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.ex.ToolbarLabelAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.vcs.api.localize.VcsApiLocalize;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VcsToolbarLabelAction extends ToolbarLabelAction implements CustomComponentAction {
  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);

    Project project = e.getProject();
    e.getPresentation().setVisible(project != null && ProjectLevelVcsManager.getInstance(project).hasActiveVcss());
    e.getPresentation().setTextValue(getConsolidatedVcsName(project));
  }

  @Nonnull
  private static LocalizeValue getConsolidatedVcsName(@Nullable Project project) {
    LocalizeValue name = VcsApiLocalize.vcsCommonLabelsVcs();
    if (project != null) {
      name = ProjectLevelVcsManager.getInstance(project).getConsolidatedVcsName().map(Presentation.NO_MNEMONIC).map((l, s) -> s + ":");
    }
    return name;
  }
}

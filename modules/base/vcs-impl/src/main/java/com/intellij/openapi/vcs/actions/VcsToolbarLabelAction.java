// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vcs.actions;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.vcs.api.localize.VcsApiLocalize;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

public class VcsToolbarLabelAction extends DumbAwareAction implements CustomComponentAction {

  private static final String DEFAULT_LABEL = "VCS:";

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabled(false);
    Project project = e.getProject();
    e.getPresentation().setVisible(project != null && ProjectLevelVcsManager.getInstance(project).hasActiveVcss());
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    //do nothing
  }

  @Nonnull
  @Override
  public JComponent createCustomComponent(Presentation presentation, String place) {
    return new VcsToolbarLabel();
  }

  private static class VcsToolbarLabel extends JBLabel {
    public VcsToolbarLabel() {
      super(DEFAULT_LABEL);
      setFont(JBUI.Fonts.toolbarFont());
      setBorder(JBUI.Borders.empty(0, 6, 0, 5));
    }

    @Override
    public String getText() {
      Project project = DataManager.getInstance().getDataContext(this).getData(CommonDataKeys.PROJECT);
      return getConsolidatedVcsName(project).get();
    }
  }

  private static LocalizeValue getConsolidatedVcsName(@Nullable Project project) {
    LocalizeValue name = VcsApiLocalize.vcsCommonLabelsVcs();
    if (project != null) {
      name = ProjectLevelVcsManager.getInstance(project).getConsolidatedVcsName().map(Presentation.NO_MNEMONIC).map((l, s) -> s + ":");
    }
    return name;
  }
}

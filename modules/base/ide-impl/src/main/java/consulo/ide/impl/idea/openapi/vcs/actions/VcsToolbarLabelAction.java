// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.ide.impl.idea.openapi.actionSystem.ex.ToolbarLabelAction;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.action.CustomComponentAction;
import consulo.versionControlSystem.localize.VcsLocalize;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class VcsToolbarLabelAction extends ToolbarLabelAction implements CustomComponentAction {
  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);

    Project project = e.getData(Project.KEY);
    e.getPresentation().setVisible(project != null && ProjectLevelVcsManager.getInstance(project).hasActiveVcss());
    e.getPresentation().setTextValue(getConsolidatedVcsName(project));
  }

  @Nonnull
  private static LocalizeValue getConsolidatedVcsName(@Nullable Project project) {
    LocalizeValue name = VcsLocalize.vcsCommonLabelsVcs();
    if (project != null) {
      name = ProjectLevelVcsManager.getInstance(project).getConsolidatedVcsName().map(Presentation.NO_MNEMONIC).map((l, s) -> s + ":");
    }
    return name;
  }
}

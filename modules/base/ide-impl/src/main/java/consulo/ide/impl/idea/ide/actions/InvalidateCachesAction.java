// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.actions.InvalidateCacheDialog;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

public class InvalidateCachesAction extends AnAction implements DumbAware {
  private final Application myApplication;

  public InvalidateCachesAction(Application application) {
    myApplication = application;
    getTemplatePresentation().setText(application.isRestartCapable() ? "Invalidate Caches / Restart..." : "Invalidate Caches...");
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    new InvalidateCacheDialog(myApplication, project).showAsync();
  }
}

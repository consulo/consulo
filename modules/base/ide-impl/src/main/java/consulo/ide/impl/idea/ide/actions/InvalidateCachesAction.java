// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.application.ApplicationManager;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.actions.InvalidateCacheDialog;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;

public class InvalidateCachesAction extends AnAction implements DumbAware {
  public InvalidateCachesAction() {
    getTemplatePresentation().setText(ApplicationManager.getApplication().isRestartCapable() ? "Invalidate Caches / Restart..." : "Invalidate Caches...");
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    new InvalidateCacheDialog(e.getData(CommonDataKeys.PROJECT)).showAsync();
  }
}

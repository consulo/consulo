// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import consulo.ide.actions.InvalidateCacheDialog;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;

public class InvalidateCachesAction extends AnAction implements DumbAware {
  public InvalidateCachesAction() {
    getTemplatePresentation().setText(ApplicationManager.getApplication().isRestartCapable() ? "Invalidate Caches / Restart..." : "Invalidate Caches...");
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    new InvalidateCacheDialog(e.getProject()).showAsync();
  }
}

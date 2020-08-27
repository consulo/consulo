// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.activity;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.runAnything.RunAnythingUtil;
import com.intellij.ide.actions.runAnything.items.RunAnythingActionItem;
import com.intellij.ide.actions.runAnything.items.RunAnythingItem;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.wm.IdeFocusManager;
import consulo.awt.TargetAWT;
import consulo.ui.image.Image;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class RunAnythingAnActionProvider<V extends AnAction> extends RunAnythingProviderBase<V> {
  @Nonnull
  @Override
  public RunAnythingItem getMainListItem(@Nonnull DataContext dataContext, @Nonnull V value) {
    return new RunAnythingActionItem<>(value, getCommand(value), value.getTemplatePresentation().getIcon());
  }

  @Override
  public void execute(@Nonnull DataContext dataContext, @Nonnull V value) {
    performRunAnythingAction(value, dataContext);
  }

  @Nullable
  @Override
  public Image getIcon(@Nonnull V value) {
    return value.getTemplatePresentation().getIcon();
  }

  private static void performRunAnythingAction(@Nonnull AnAction action, @Nonnull DataContext dataContext) {
    ApplicationManager.getApplication().invokeLater(() -> IdeFocusManager.getInstance(RunAnythingUtil.fetchProject(dataContext)).doWhenFocusSettlesDown(() -> performAction(action, dataContext)));
  }

  private static void performAction(@Nonnull AnAction action, @Nonnull DataContext dataContext) {
    AnActionEvent event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, dataContext);

    ActionUtil.performActionDumbAwareWithCallbacks(action, event, dataContext);
  }

  @Nullable
  @Override
  public String getAdText() {
    return IdeBundle.message("run.anything.ad.run.action.with.default.settings", RunAnythingUtil.SHIFT_SHORTCUT_TEXT);
  }
}

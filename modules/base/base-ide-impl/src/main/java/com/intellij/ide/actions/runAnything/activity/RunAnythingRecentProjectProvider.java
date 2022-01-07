// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.activity;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.RecentProjectsManager;
import com.intellij.ide.ReopenProjectAction;
import com.intellij.ide.actions.runAnything.RunAnythingContext;
import com.intellij.ide.actions.runAnything.items.RunAnythingItem;
import com.intellij.ide.actions.runAnything.items.RunAnythingItemBase;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class RunAnythingRecentProjectProvider extends RunAnythingAnActionProvider<AnAction> {
  @Nonnull
  @Override
  public Collection<AnAction> getValues(@Nonnull DataContext dataContext, @Nonnull String pattern) {
    return Arrays.stream(RecentProjectsManager.getInstance().getRecentProjectsActions(false)).collect(Collectors.toList());
  }

  @Nonnull
  @Override
  public RunAnythingItem getMainListItem(@Nonnull DataContext dataContext, @Nonnull AnAction value) {
    if (value instanceof ReopenProjectAction) {
      return new RecentProjectElement(((ReopenProjectAction)value), getCommand(value), ((ReopenProjectAction)value).getExtensionIcon());
    }
    return super.getMainListItem(dataContext, value);
  }

  @Override
  @Nonnull
  public String getCompletionGroupTitle() {
    return IdeBundle.message("run.anything.recent.project.completion.group.title");
  }

  @Nonnull
  @Override
  public String getHelpCommandPlaceholder() {
    return IdeBundle.message("run.anything.recent.project.command.placeholder");
  }

  @Nonnull
  @Override
  public String getHelpCommand() {
    return "open";
  }

  @Nullable
  @Override
  public String getHelpGroupTitle() {
    return IdeBundle.message("run.anything.recent.project.help.group.title");
  }

  @Nonnull
  @Override
  public String getCommand(@Nonnull AnAction value) {
    return getHelpCommand() + " " + ObjectUtils.notNull(value.getTemplatePresentation().getText(), IdeBundle.message("run.anything.actions.undefined"));
  }

  static class RecentProjectElement extends RunAnythingItemBase {
    @Nonnull
    private final ReopenProjectAction myValue;

    RecentProjectElement(@Nonnull ReopenProjectAction value, @Nonnull String command, @Nullable Image icon) {
      super(command, icon);
      myValue = value;
    }

    @Nullable
    @Override
    public String getDescription() {
      return myValue.getProjectPath();
    }
  }

  @Nonnull
  @Override
  public List<RunAnythingContext> getExecutionContexts(@Nonnull DataContext dataContext) {
    return ContainerUtil.emptyList();
  }
}
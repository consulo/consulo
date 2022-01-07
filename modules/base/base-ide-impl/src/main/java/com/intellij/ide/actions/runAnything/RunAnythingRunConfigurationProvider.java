// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything;

import com.intellij.execution.actions.ChooseRunConfigurationPopup;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

import static com.intellij.ide.actions.runAnything.RunAnythingUtil.fetchProject;

public class RunAnythingRunConfigurationProvider extends com.intellij.ide.actions.runAnything.activity.RunAnythingRunConfigurationProvider {
  @Nonnull
  @Override
  public Collection<ChooseRunConfigurationPopup.ItemWrapper> getValues(@Nonnull DataContext dataContext, @Nonnull String pattern) {
    return getWrappers(dataContext);
  }

  @Nullable
  @Override
  public String getHelpGroupTitle() {
    return null;
  }

  @Nonnull
  @Override
  public String getCompletionGroupTitle() {
    return IdeBundle.message("run.anything.run.configurations.group.title");
  }

  @Nonnull
  private static List<ChooseRunConfigurationPopup.ItemWrapper> getWrappers(@Nonnull DataContext dataContext) {
    Project project = fetchProject(dataContext);
    return ChooseRunConfigurationPopup.createFlatSettingsList(project);
  }

  @Nonnull
  @Override
  public List<RunAnythingContext> getExecutionContexts(@Nonnull DataContext dataContext) {
    return ContainerUtil.emptyList();
  }
}
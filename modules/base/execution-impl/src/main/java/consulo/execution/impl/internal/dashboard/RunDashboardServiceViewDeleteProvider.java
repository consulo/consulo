// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.impl.internal.dashboard;

import consulo.application.CommonBundle;
import consulo.dataContext.DataContext;
import consulo.execution.ExecutionBundle;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.dashboard.RunDashboardGroup;
import consulo.execution.dashboard.RunDashboardManager;
import consulo.execution.impl.internal.dashboard.tree.GroupingNode;
import consulo.execution.impl.internal.dashboard.tree.RunDashboardGroupImpl;
import consulo.execution.service.ServiceViewContributorDeleteProvider;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.project.Project;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.awt.MessageDialogBuilder;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class RunDashboardServiceViewDeleteProvider implements ServiceViewContributorDeleteProvider {
  private DeleteProvider myDelegate;

  @Override
  public void setFallbackProvider(DeleteProvider provider) {
    myDelegate = provider;
  }

//  @Override
//  public @NotNull ActionUpdateThread getActionUpdateThread() {
//    return ActionUpdateThread.EDT;
//  }

  @Override
  public void deleteElement(@Nonnull DataContext dataContext) {
    List<ConfigurationType> targetTypes = getTargetTypes(dataContext);
    if (targetTypes.isEmpty()) {
      if (myDelegate != null) {
        myDelegate.deleteElement(dataContext);
      }
      return;
    }

    Project project = dataContext.getData(CommonDataKeys.PROJECT);
    if (project == null) return;

    ConfigurationType onlyType = ContainerUtil.getOnlyItem(targetTypes);
    String message;
    if (onlyType != null) {
      message = ExecutionBundle.message("run.dashboard.remove.run.configuration.type.confirmation", onlyType.getDisplayName());
    }
    else {
      message = ExecutionBundle.message("run.dashboard.remove.run.configuration.types.confirmation", targetTypes.size());
    }

    if (!MessageDialogBuilder.yesNo(CommonBundle.message("button.remove"), message)
                             .yesText(CommonBundle.message("button.remove"))
                             .icon(Messages.getWarningIcon())
                             .project(project)
                             .isYes()) {
      return;
    }
    RunDashboardManager runDashboardManager = RunDashboardManager.getInstance(project);
    Set<String> types = new HashSet<>(runDashboardManager.getTypes());
    for (ConfigurationType type : targetTypes) {
      types.remove(type.getId());
    }
    runDashboardManager.setTypes(types);
  }

  @Override
  public boolean canDeleteElement(@Nonnull DataContext dataContext) {
    List<ConfigurationType> targetTypes = getTargetTypes(dataContext);
    return !targetTypes.isEmpty() || (myDelegate != null && myDelegate.canDeleteElement(dataContext));
  }

  private static List<ConfigurationType> getTargetTypes(DataContext dataContext) {
    Object[] items = dataContext.getData(PlatformDataKeys.SELECTED_ITEMS);
    if (items == null) return Collections.emptyList();

    List<ConfigurationType> types = new SmartList<>();
    for (Object item : items) {
      if (item instanceof GroupingNode) {
        RunDashboardGroup group = ((GroupingNode)item).getGroup();
        ConfigurationType type = ObjectUtil.tryCast(((RunDashboardGroupImpl<?>)group).getValue(), ConfigurationType.class);
        if (type != null) {
          types.add(type);
          continue;
        }
      }
      return Collections.emptyList();
    }
    return types;
  }
}

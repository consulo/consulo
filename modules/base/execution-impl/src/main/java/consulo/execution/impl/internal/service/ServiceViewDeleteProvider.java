// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.service;

import consulo.dataContext.DataContext;
import consulo.execution.localize.ExecutionLocalize;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.List;

final class ServiceViewDeleteProvider implements DeleteProvider {
  private final ServiceView myServiceView;

  ServiceViewDeleteProvider(@Nonnull ServiceView serviceView) {
    myServiceView = serviceView;
  }

//  @Override
//  public @NotNull ActionUpdateThread getActionUpdateThread() {
//    return ActionUpdateThread.EDT;
//  }

  @Override
  @RequiredUIAccess
  public void deleteElement(@Nonnull DataContext dataContext) {
    Project project = dataContext.getData(Project.KEY);
    if (project == null) return;

    List<ServiceViewItem> selectedItems = ServiceViewActionProvider.getSelectedItems(dataContext);
    List<Pair<ServiceViewItem, Runnable>> items = ContainerUtil.mapNotNull(selectedItems, item -> {
      Runnable remover = item.getViewDescriptor().getRemover();
      return remover == null ? null : Pair.create(item, remover);
    });
    items = filterChildren(items);
    if (items.isEmpty()) return;

    if (Messages.showYesNoDialog(
      project,
      ExecutionLocalize.serviceViewDeleteConfirmation(ExecutionLocalize.serviceViewItems(items.size()).get()).get(),
      CommonLocalize.buttonDelete().get(),
      UIUtil.getWarningIcon())
      != Messages.YES
    ) {
      return;
    }
    for (Pair<ServiceViewItem, Runnable> item : items) {
      item.second.run();
    }
  }

  @Override
  public boolean canDeleteElement(@Nonnull DataContext dataContext) {
    List<ServiceViewItem> selectedItems = ServiceViewActionProvider.getSelectedItems(dataContext);
    if (!ContainerUtil.exists(selectedItems, item -> item.getViewDescriptor().getRemover() != null)) {
      return false;
    }
    JComponent detailsComponent = myServiceView.getUi().getDetailsComponent();
    return detailsComponent == null || !UIUtil.isAncestor(detailsComponent, dataContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT));
  }

  @Nonnull
  private static List<Pair<ServiceViewItem, Runnable>> filterChildren(List<? extends Pair<ServiceViewItem, Runnable>> items) {
    return ContainerUtil.filter(items, item -> {
      ServiceViewItem parent = item.first.getParent();
      while (parent != null) {
        for (Pair<ServiceViewItem, Runnable> pair : items) {
          if (pair.first.equals(parent)) {
            return false;
          }
        }
        parent = parent.getParent();
      }
      return true;
    });
  }
}
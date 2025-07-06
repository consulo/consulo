// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.service.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.execution.impl.internal.service.ServiceView;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import jakarta.annotation.Nonnull;

import static consulo.execution.impl.internal.service.ServiceViewActionProvider.getSelectedView;

@ActionImpl(id = "ServiceView.GroupByServiceGroup")
public final class GroupByServiceGroupsAction extends ToggleAction implements DumbAware {
  public GroupByServiceGroupsAction() {
    super(ActionLocalize.actionServiceviewGroupbyservicegroupsText());
  }

//  @Override
//  public @NotNull ActionUpdateThread getActionUpdateThread() {
//    return ActionUpdateThread.BGT;
//  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);
    e.getPresentation().setEnabled(getSelectedView(e) != null);
  }

  @Override
  public boolean isSelected(@Nonnull AnActionEvent e) {
    ServiceView selectedView = getSelectedView(e);
    return selectedView != null && selectedView.isGroupByServiceGroups();
  }

  @Override
  public void setSelected(@Nonnull AnActionEvent e, boolean state) {
    ServiceView selectedView = getSelectedView(e);
    if (selectedView != null) {
      selectedView.setGroupByServiceGroups(state);
    }
  }
}

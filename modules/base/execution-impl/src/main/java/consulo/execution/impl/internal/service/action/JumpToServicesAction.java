// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.service.action;

import consulo.annotation.component.ActionImpl;
import consulo.execution.impl.internal.service.ServiceView;
import consulo.execution.impl.internal.service.ServiceViewModel;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;

import static consulo.execution.impl.internal.service.ServiceViewActionProvider.getSelectedView;

@ActionImpl(id = "ServiceView.JumpToServices")
public final class JumpToServicesAction extends DumbAwareAction {

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    ServiceView selectedView = getSelectedView(e);
    if (selectedView == null) return;

    selectedView.jumpToServices();
  }

//  @Override
//  public @NotNull ActionUpdateThread getActionUpdateThread() {
//    return ActionUpdateThread.BGT;
//  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    presentation.setVisible(false);
    ServiceView selectedView = getSelectedView(e);
    presentation.setEnabled(selectedView != null && !(selectedView.getModel() instanceof ServiceViewModel.SingeServiceModel));
  }
}

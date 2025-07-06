// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.service.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import jakarta.annotation.Nonnull;

import static consulo.execution.impl.internal.service.ServiceViewActionProvider.getSelectedView;

@ActionImpl(id = "ServiceView.AddService")
public final class AddServiceActionGroup extends DefaultActionGroup implements DumbAware {
  public AddServiceActionGroup() {
    super(ActionLocalize.groupServiceviewAddserviceText(), LocalizeValue.empty(), PlatformIconGroup.generalAdd());
  }

  @Override
  public boolean isPopup() {
    return true;
  }

//  @Override
//  public @NotNull ActionUpdateThread getActionUpdateThread() {
//    return ActionUpdateThread.BGT;
//  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabled(getSelectedView(e) != null);
  }
}

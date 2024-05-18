// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.service.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static consulo.execution.impl.internal.service.ServiceViewActionProvider.getSelectedView;

@ActionImpl(id = "ServiceView.AddService")
public final class AddServiceActionGroup extends DefaultActionGroup implements DumbAware {

  public AddServiceActionGroup() {

  }

  @Nullable
  @Override
  protected Image getTemplateIcon() {
    return PlatformIconGroup.generalAdd();
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

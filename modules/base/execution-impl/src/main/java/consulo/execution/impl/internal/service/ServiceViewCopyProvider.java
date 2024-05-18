// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.service;

import consulo.dataContext.DataContext;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;

final class ServiceViewCopyProvider implements CopyProvider {
  private final ServiceView myServiceView;

  ServiceViewCopyProvider(@Nonnull ServiceView serviceView) {
    myServiceView = serviceView;
  }

//  @Override
//  public @NotNull ActionUpdateThread getActionUpdateThread() {
//    return ActionUpdateThread.EDT;
//  }

  @Override
  public void performCopy(@Nonnull DataContext dataContext) {
    List<ServiceViewItem> items = ServiceViewActionProvider.getSelectedItems(dataContext);
    if (!items.isEmpty()) {
      CopyPasteManager.getInstance().setContents(new StringSelection(
        StringUtil.join(items, item -> ServiceViewDragHelper.getDisplayName(item.getViewDescriptor().getPresentation()), "\n")));
    }
  }

  @Override
  public boolean isCopyEnabled(@Nonnull DataContext dataContext) {
    if (ServiceViewActionProvider.getSelectedItems(dataContext).isEmpty()) {
      return false;
    }
    JComponent detailsComponent = myServiceView.getUi().getDetailsComponent();
    return detailsComponent == null || !UIUtil.isAncestor(detailsComponent, dataContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT));
  }

  @Override
  public boolean isCopyVisible(@Nonnull DataContext dataContext) {
    return false;
  }
}
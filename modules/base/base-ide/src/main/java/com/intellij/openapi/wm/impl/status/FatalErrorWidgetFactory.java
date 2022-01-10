// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.wm.impl.status;

import com.intellij.diagnostic.IdeMessagePanel;
import com.intellij.diagnostic.MessagePool;
import com.intellij.openapi.project.Project;
import consulo.disposer.Disposer;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.UIBundle;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;

public class FatalErrorWidgetFactory implements StatusBarWidgetFactory {
  @Override
  public
  @Nonnull
  String getId() {
    return IdeMessagePanel.FATAL_ERROR;
  }

  @Override
  public
  @Nls
  @Nonnull
  String getDisplayName() {
    return UIBundle.message("status.bar.fatal.error.widget.name");
  }

  @Override
  public boolean isAvailable(@Nonnull Project project) {
    return true;
  }

  @Override
  public
  @Nonnull
  StatusBarWidget createWidget(@Nonnull Project project) {
    return new IdeMessagePanel(WindowManager.getInstance().getIdeFrame(project), MessagePool.getInstance());
  }

  @Override
  public void disposeWidget(@Nonnull StatusBarWidget widget) {
    Disposer.dispose(widget);
  }

  @Override
  public boolean isConfigurable() {
    return false;
  }

  @Override
  public boolean canBeEnabledOn(@Nonnull StatusBar statusBar) {
    return false;
  }
}

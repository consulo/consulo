// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.usageView;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import javax.annotation.Nonnull;

import javax.swing.*;

/**
 * @author yole
 */
public abstract class UsageViewContentManager {
  public static UsageViewContentManager getInstance(Project project) {
    return ServiceManager.getService(project, UsageViewContentManager.class);
  }

  @Nonnull
  public abstract Content addContent(@Nonnull String contentName, boolean reusable, @Nonnull JComponent component, boolean toOpenInNewTab, boolean isLockable);

  @Nonnull
  public abstract Content addContent(@Nonnull String contentName, String tabName, String toolwindowTitle, boolean reusable, @Nonnull JComponent component, boolean toOpenInNewTab, boolean isLockable);

  public abstract int getReusableContentsCount();

  public abstract Content getSelectedContent(boolean reusable);

  public abstract Content getSelectedContent();

  public abstract void closeContent(@Nonnull Content usageView);
}

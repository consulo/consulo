// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.usageView;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import javax.annotation.Nonnull;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javax.swing.*;

/**
 * @deprecated use {@link UsageViewContentManager} instead
 */
@Singleton
@Deprecated
public class UsageViewManager {
  private final UsageViewContentManager myUsageViewContentManager;

  @Inject
  public UsageViewManager(@Nonnull UsageViewContentManager manager) {
    myUsageViewContentManager = manager;
  }

  public static UsageViewManager getInstance(Project project) {
    return ServiceManager.getService(project, UsageViewManager.class);
  }

  @Nonnull
  public Content addContent(String contentName, String tabName, String toolwindowTitle, boolean reusable, final JComponent component, boolean toOpenInNewTab, boolean isLockable) {
    return myUsageViewContentManager.addContent(contentName, tabName, toolwindowTitle, reusable, component, toOpenInNewTab, isLockable);
  }

  public Content getSelectedContent() {
    return myUsageViewContentManager.getSelectedContent();
  }
}
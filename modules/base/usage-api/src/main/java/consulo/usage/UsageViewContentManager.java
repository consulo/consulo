// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.usage;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.ui.ex.content.Content;

import jakarta.annotation.Nonnull;
import javax.swing.*;

/**
 * @author yole
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class UsageViewContentManager {
  public static UsageViewContentManager getInstance(Project project) {
    return project.getInstance(UsageViewContentManager.class);
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

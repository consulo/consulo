// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.content.Content;
import consulo.disposer.Disposable;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * @author Vladislav.Soroka
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface BuildContentManager {
  String TOOL_WINDOW_ID = "Build";

  @Nonnull
  static BuildContentManager getInstance(@Nonnull Project project) {
    return project.getInstance(BuildContentManager.class);
  }

  void addContent(Content content);

  @Nonnull
  ToolWindow getOrCreateToolWindow();

  void removeContent(Content content);

  Content addTabbedContent(@Nonnull JComponent contentComponent,
                           @Nonnull String groupPrefix,
                           @Nonnull String tabName,
                           @Nullable Image icon,
                           @Nullable Disposable childDisposable);

  void setSelectedContent(Content content,
                          boolean requestFocus,
                          boolean forcedFocus,
                          boolean activate,
                          Runnable activationCallback);
}

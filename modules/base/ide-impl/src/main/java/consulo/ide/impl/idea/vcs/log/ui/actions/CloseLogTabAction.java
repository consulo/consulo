/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.idea.vcs.log.ui.actions;

import consulo.ui.ex.content.ContentUtilEx;
import consulo.ide.impl.idea.vcs.log.impl.VcsLogContentProvider;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CloseTabToolbarAction;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.ContentsUtil;
import consulo.ui.ex.toolWindow.ToolWindow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class CloseLogTabAction extends CloseTabToolbarAction {
  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);
    if (!e.hasData(Project.KEY)) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }
    ContentManager contentManager = getContentManager(e.getRequiredData(Project.KEY));
    if (contentManager == null || getTabbedContent(contentManager) == null) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }
    e.getPresentation().setEnabledAndVisible(true);
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    ContentManager contentManager = getContentManager(e.getRequiredData(Project.KEY));
    if (contentManager == null) return;
    Content selectedContent = getTabbedContent(contentManager);
    if (selectedContent != null) {
      ContentsUtil.closeContentTab(contentManager, selectedContent);
    }
  }

  @Nullable
  private static Content getTabbedContent(@Nonnull ContentManager contentManager) {
    Content content = contentManager.getSelectedContent();
    if (content != null) {
      if (ContentUtilEx.isContentTab(content, VcsLogContentProvider.TAB_NAME)) return content;
    }
    return null;
  }

  @Nullable
  private static ContentManager getContentManager(@Nonnull Project project) {
    ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.VCS);
    if (toolWindow == null) return null;
    return toolWindow.getContentManager();
  }
}

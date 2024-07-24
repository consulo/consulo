/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.project.ui.wm;

import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.project.ui.internal.ToolWindowManagerEx;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.internal.ToolWindowEx;

public class ContentManagerUtil {
  private ContentManagerUtil() {
  }

  /**
   * This is utility method. It returns <code>ContentManager</code> from the current context.
   */
  @RequiredUIAccess
  public static ContentManager getContentManagerFromContext(DataContext dataContext, boolean requiresVisibleToolWindow) {
    Project project = dataContext.getData(Project.KEY);
    if (project == null) {
      return null;
    }

    ToolWindowManagerEx mgr = ToolWindowManagerEx.getInstanceEx(project);

    String id = mgr.getActiveToolWindowId();
    if (id == null && mgr.isEditorComponentActive()) {
      id = mgr.getLastActiveToolWindowId();
    }
    if (id == null) {
      return null;
    }

    ToolWindowEx toolWindow = (ToolWindowEx)mgr.getToolWindow(id);
    if (requiresVisibleToolWindow && !toolWindow.isVisible()) {
      return null;
    }

    final ContentManager fromContext = dataContext.getData(ContentManager.KEY);
    if (fromContext != null) return fromContext;

    return toolWindow != null ? toolWindow.getContentManager() : null;
  }
}

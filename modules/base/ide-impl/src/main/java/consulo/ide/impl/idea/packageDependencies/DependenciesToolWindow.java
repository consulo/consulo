/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.packageDependencies;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.ide.ServiceManager;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.startup.StartupManager;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ContentManagerWatcher;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author yole
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class DependenciesToolWindow {
  public static DependenciesToolWindow getInstance(Project project) {
    return ServiceManager.getService(project, DependenciesToolWindow.class);
  }

  private final Project myProject;
  private ContentManager myContentManager;

  @Inject
  public DependenciesToolWindow(final Project project, StartupManager startupManager) {
    myProject = project;
    startupManager.runWhenProjectIsInitialized(new Runnable() {
      @Override
      public void run() {
        final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
        if (toolWindowManager == null) return;
        ToolWindow toolWindow = toolWindowManager.registerToolWindow(ToolWindowId.DEPENDENCIES, true, ToolWindowAnchor.BOTTOM, project);
        myContentManager = toolWindow.getContentManager();

        toolWindow.setIcon(PlatformIconGroup.toolwindowsToolwindowmoduledependencies());
        new ContentManagerWatcher(toolWindow, myContentManager);
      }
    });
  }

  public void addContent(Content content) {
    myContentManager.addContent(content);
    myContentManager.setSelectedContent(content);
    ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.DEPENDENCIES).activate(null);
  }

  public void closeContent(Content content) {
    myContentManager.removeContent(content, true);
  }
}

/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.ide.impl.idea.moduleDependencies;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.AllIcons;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.ide.ServiceManager;
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
 * @author anna
 * @since 2005-02-10
 */
@Singleton
@State(name = "DependenciesAnalyzeManager", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class DependenciesAnalyzeManager implements PersistentStateComponent<DependenciesAnalyzeManager.State> {
  private final Project myProject;
  private ContentManager myContentManager;

  public static class State {
    public boolean myForwardDirection;
  }

  private State myState;

  @Inject
  public DependenciesAnalyzeManager(final Project project, StartupManager startupManager) {
    myProject = project;
    startupManager.runWhenProjectIsInitialized(new Runnable() {
      @Override
      public void run() {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
        ToolWindow toolWindow = toolWindowManager.registerToolWindow(ToolWindowId.MODULES_DEPENDENCIES, true, ToolWindowAnchor.RIGHT, project);
        myContentManager = toolWindow.getContentManager();
        toolWindow.setIcon(AllIcons.Toolwindows.ToolWindowModuleDependencies);
        new ContentManagerWatcher(toolWindow, myContentManager);
      }
    });
  }

  public static DependenciesAnalyzeManager getInstance(Project project) {
    return ServiceManager.getService(project, DependenciesAnalyzeManager.class);
  }

  public void addContent(Content content) {
    myContentManager.addContent(content);
    myContentManager.setSelectedContent(content);
    ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.MODULES_DEPENDENCIES).activate(null);
  }

  public void closeContent(Content content) {
    myContentManager.removeContent(content, true);
  }

  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(final State state) {
    myState = state;
  }
}

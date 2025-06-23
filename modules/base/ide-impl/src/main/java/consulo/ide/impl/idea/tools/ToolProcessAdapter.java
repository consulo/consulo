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
package consulo.ide.impl.idea.tools;

import consulo.process.event.ProcessAdapter;
import consulo.process.event.ProcessEvent;
import consulo.application.ApplicationManager;
import consulo.project.Project;
import consulo.project.internal.ProjectManagerEx;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.WindowManager;

/**
 * @author Eugene Zhuravlev
 * @since 2005-03-30
 */
public class ToolProcessAdapter extends ProcessAdapter {
  private final Project myProject;
  private final boolean mySynchronizeAfterExecution;
  private final String myName;

  public ToolProcessAdapter(Project project, final boolean synchronizeAfterExecution, final String name) {
    myProject = project;
    mySynchronizeAfterExecution = synchronizeAfterExecution;
    myName = name;
  }

  @Override
  public void processTerminated(ProcessEvent event) {
    final String message = ToolsBundle.message("tools.completed.message", myName, event.getExitCode());

    if (mySynchronizeAfterExecution) {
      ApplicationManager.getApplication().runReadAction(new Runnable() {
        @Override
        public void run() {
          VirtualFileManager.getInstance().asyncRefresh(new Runnable() {
            @Override
            public void run() {
              if (ProjectManagerEx.getInstanceEx().isProjectOpened(myProject)) {
                StatusBar statusBar = WindowManager.getInstance().getStatusBar(myProject);
                if (statusBar != null) {
                  statusBar.setInfo(message);
                }
              }
            }
          });
        }
      });
    }
    if (ProjectManagerEx.getInstanceEx().isProjectOpened(myProject)) {
      StatusBar statusBar = WindowManager.getInstance().getStatusBar(myProject);
      if (statusBar != null) {
        statusBar.setInfo(message);
      }
    }
  }
}

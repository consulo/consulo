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
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesViewContentManager;
import consulo.project.Project;
import consulo.project.ui.view.SelectInContext;
import consulo.project.ui.view.SelectInTarget;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsBundle;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.inject.Inject;

import javax.annotation.Nullable;

/**
 * @author yole
 */
@ExtensionImpl
public class SelectInChangesViewTarget implements SelectInTarget, DumbAware {
  private final Project myProject;

  @Inject
  public SelectInChangesViewTarget(final Project project) {
    myProject = project;
  }

  public String toString() {
    return VcsBundle.message("changes.toolwindow.name");
  }

  public boolean canSelect(final SelectInContext context) {
    final VirtualFile file = context.getVirtualFile();
    FileStatus fileStatus = FileStatusManager.getInstance(myProject).getStatus(file);
    return ProjectLevelVcsManager.getInstance(myProject).getAllActiveVcss().length != 0 &&
           !fileStatus.equals(FileStatus.NOT_CHANGED);
  }

  public void selectIn(final SelectInContext context, final boolean requestFocus) {
    final VirtualFile file = context.getVirtualFile();
    Runnable runnable = new Runnable() {
      public void run() {
        ChangesViewContentManager.getInstance(myProject).selectContent("Local");
        ChangesViewManager.getInstance(myProject).selectFile(file);
      }
    };
    if (requestFocus) {
      ToolWindowManager.getInstance(myProject).getToolWindow(ChangesViewContentManager.TOOLWINDOW_ID).activate(runnable);
    }
    else {
      runnable.run();
    }
  }

  public String getToolWindowId() {
    return ChangesViewContentManager.TOOLWINDOW_ID;
  }

  @Nullable public String getMinorViewId() {
    return null;
  }

  public float getWeight() {
    return 9;
  }
}

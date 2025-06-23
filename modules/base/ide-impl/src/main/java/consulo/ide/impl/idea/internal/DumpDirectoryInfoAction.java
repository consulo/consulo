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
package consulo.ide.impl.idea.internal;

import consulo.application.progress.ProgressManager;
import consulo.content.ContentIterator;
import consulo.logging.Logger;
import consulo.module.content.DirectoryIndex;
import consulo.module.content.DirectoryInfo;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.VirtualFile;

/**
 * @author anna
 * @since 2012-02-24
 */
public class DumpDirectoryInfoAction extends AnAction {
  public static final Logger LOG = Logger.getInstance(DumpDirectoryInfoAction.class);

  public DumpDirectoryInfoAction() {
    super("Dump Directory Info");
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getData(Project.KEY);
    final DirectoryIndex index = DirectoryIndex.getInstance(project);
    if (project != null) {
      final VirtualFile root = e.getData(VirtualFile.KEY);
      ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
        final ContentIterator contentIterator = fileOrDir -> {
          LOG.info(fileOrDir.getPath());

          final DirectoryInfo directoryInfo = index.getInfoForDirectory(fileOrDir);
          if (directoryInfo != null) {
            LOG.info(directoryInfo.toString());
          }
          return true;
        };
        if (root != null) {
          ProjectRootManager.getInstance(project).getFileIndex().iterateContentUnderDirectory(root, contentIterator);
        } else {
          ProjectRootManager.getInstance(project).getFileIndex().iterateContent(contentIterator);
        }
      }, "Dumping directory index", true, project);
    }
  }

  @Override
  @RequiredUIAccess
  public void update(AnActionEvent e) {
    e.getPresentation().setEnabled(e.getData(Project.KEY) != null);
  }
}

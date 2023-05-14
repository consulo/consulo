/*
 * Copyright 2013-2016 consulo.io
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
package consulo.virtualFileSystem.fileWatcher.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.TreeStructureProvider;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileWatcher.BackgroundTaskByVfsChangeManager;
import consulo.virtualFileSystem.fileWatcher.BackgroundTaskByVfsChangeTask;
import jakarta.inject.Inject;

import java.util.*;

/**
 * @author VISTALL
 * @since 04.03.14
 */
@ExtensionImpl
public class BackgroundTaskByVfsProjectViewProvider implements TreeStructureProvider, DumbAware {
  private final Project myProject;
  private final BackgroundTaskByVfsChangeManager myBackgroundTaskByVfsChangeManager;

  @Inject
  public BackgroundTaskByVfsProjectViewProvider(Project project, BackgroundTaskByVfsChangeManager backgroundTaskByVfsChangeManager) {
    myProject = project;
    myBackgroundTaskByVfsChangeManager = backgroundTaskByVfsChangeManager;
  }

  @Override
  @RequiredUIAccess
  public Collection<AbstractTreeNode> modify(AbstractTreeNode parent, Collection<AbstractTreeNode> children, ViewSettings settings) {
    if (parent instanceof BackgroundTaskPsiFileTreeNode) {
      return children;
    }

    List<VirtualFile> allGeneratedFiles = new ArrayList<>();
    for (BackgroundTaskByVfsChangeTask o : myBackgroundTaskByVfsChangeManager.getTasks()) {
      Collections.addAll(allGeneratedFiles, o.getGeneratedFiles());
    }

    List<AbstractTreeNode> list = new ArrayList<>(children);
    for (ListIterator<AbstractTreeNode> iterator = list.listIterator(); iterator.hasNext(); ) {
      AbstractTreeNode next = iterator.next();

      Object value = next.getValue();
      if (value instanceof PsiFile) {
        VirtualFile virtualFile = ((PsiFile)value).getVirtualFile();
        if (virtualFile == null) {
          continue;
        }

        if (allGeneratedFiles.contains(virtualFile)) {
          iterator.remove();
        }
        else if (!myBackgroundTaskByVfsChangeManager.findTasks(virtualFile).isEmpty()) {
          iterator.set(new BackgroundTaskPsiFileTreeNode(myProject, (PsiFile)value, settings));
        }
      }
    }
    return list;
  }
}

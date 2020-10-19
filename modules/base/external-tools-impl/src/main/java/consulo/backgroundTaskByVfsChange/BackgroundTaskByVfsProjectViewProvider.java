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
package consulo.backgroundTaskByVfsChange;

import com.intellij.ide.projectView.SelectableTreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import consulo.backgroundTaskByVfsChange.tree.BackgroundTaskPsiFileTreeNode;
import javax.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.*;

/**
 * @author VISTALL
 * @since 04.03.14
 */
public class BackgroundTaskByVfsProjectViewProvider implements SelectableTreeStructureProvider, DumbAware {
  private final Project myProject;

  @Inject
  public BackgroundTaskByVfsProjectViewProvider(Project project) {
    myProject = project;
  }

  @Nullable
  @Override
  public PsiElement getTopLevelElement(PsiElement element) {
    return null;
  }

  @Override
  public Collection<AbstractTreeNode> modify(AbstractTreeNode parent, Collection<AbstractTreeNode> children, ViewSettings settings) {
    if (parent instanceof BackgroundTaskPsiFileTreeNode) {
      return children;
    }

    List<VirtualFile> allGeneratedFiles = new ArrayList<VirtualFile>();
    BackgroundTaskByVfsChangeManager vfsChangeManager = BackgroundTaskByVfsChangeManager.getInstance(myProject);
    for (BackgroundTaskByVfsChangeTask o : vfsChangeManager.getTasks()) {
      Collections.addAll(allGeneratedFiles, o.getGeneratedFiles());
    }

    List<AbstractTreeNode> list = new ArrayList<AbstractTreeNode>(children);
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
        else if (!vfsChangeManager.findTasks(virtualFile).isEmpty()) {
          iterator.set(new BackgroundTaskPsiFileTreeNode(myProject, (PsiFile)value, settings));
        }
      }
    }
    return list;
  }
}

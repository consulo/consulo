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

package consulo.ide.impl.idea.ide.todo.nodes;

import consulo.annotation.access.RequiredReadAction;
import consulo.ide.localize.IdeLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.tree.PresentationData;
import consulo.ide.impl.idea.ide.todo.TodoTreeBuilder;
import consulo.ide.impl.idea.ide.todo.TodoTreeStructure;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.application.ReadAction;
import consulo.module.Module;
import consulo.language.util.ModuleUtilCore;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.module.content.ModuleRootManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.search.TodoItem;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class ModuleToDoNode extends BaseToDoNode<Module> {

  public ModuleToDoNode(Project project, @Nonnull Module value, TodoTreeBuilder builder) {
    super(project, value, builder);
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public Collection<AbstractTreeNode> getChildren() {
    ArrayList<AbstractTreeNode> children = new ArrayList<>();
    if (myToDoSettings.getIsPackagesShown()) {
      TodoTreeHelper.addPackagesToChildren(children, getProject(), getValue(), myBuilder);
    }
    else {
      for (Iterator i = myBuilder.getAllFiles(); i.hasNext(); ) {
        final PsiFile psiFile = (PsiFile)i.next();
        if (psiFile == null) { // skip invalid PSI files
          continue;
        }
        final VirtualFile virtualFile = psiFile.getVirtualFile();
        final boolean isInContent = ModuleRootManager.getInstance(getValue()).getFileIndex().isInContent(virtualFile);
        if (!isInContent) continue;
        TodoFileNode fileNode = new TodoFileNode(getProject(), psiFile, myBuilder, false);
        if (getTreeStructure().accept(psiFile) && !children.contains(fileNode)) {
          children.add(fileNode);
        }
      }
    }
    return children;

  }

  @Override
  public boolean contains(Object element) {
    if (element instanceof TodoItem) {
      Module module = ModuleUtilCore.findModuleForFile(((TodoItem)element).getFile());
      return super.canRepresent(module);
    }

    if (element instanceof PsiElement) {
      Module module = ModuleUtilCore.findModuleForPsiElement((PsiElement)element);
      return super.canRepresent(module);
    }
    return super.canRepresent(element);
  }

  private TodoTreeStructure getStructure() {
    return myBuilder.getTodoTreeStructure();
  }

  @Override
  public void update(@Nonnull PresentationData presentation) {
    if (DumbService.getInstance(getProject()).isDumb()) return;
    String newName = getValue().getName();
    int todoItemCount = getTodoItemCount(getValue());
    presentation.setLocationString(IdeLocalize.nodeTodoGroup(todoItemCount));
    presentation.setIcon(PlatformIconGroup.nodesModule());
    presentation.setPresentableText(newName);
  }

  @Override
  public String getTestPresentation() {
    return "Module";
  }

  @Override
  public int getFileCount(Module module) {
    Iterator<PsiFile> iterator = myBuilder.getFiles(module);
    int count = 0;
    while (iterator.hasNext()) {
      PsiFile psiFile = iterator.next();
      if (getStructure().accept(psiFile)) {
        count++;
      }
    }
    return count;
  }

  @Override
  public int getTodoItemCount(final Module val) {
    Iterator<PsiFile> iterator = myBuilder.getFiles(val);
    int count = 0;
    while (iterator.hasNext()) {
      final PsiFile psiFile = iterator.next();
      count += ReadAction.compute(() -> getTreeStructure().getTodoItemCount(psiFile));
    }
    return count;
  }

  @Override
  public int getWeight() {
    return 1;
  }
}

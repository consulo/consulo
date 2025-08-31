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
import consulo.ui.ex.tree.PresentationData;
import consulo.ide.impl.idea.ide.todo.CurrentFileTodosTreeBuilder;
import consulo.ide.impl.idea.ide.todo.ToDoSummary;
import consulo.ide.impl.idea.ide.todo.TodoFileDirAndModuleComparator;
import consulo.ide.impl.idea.ide.todo.TodoTreeBuilder;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.application.ReadAction;
import consulo.module.Module;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public class SummaryNode extends BaseToDoNode<ToDoSummary> {
  public SummaryNode(Project project, @Nonnull ToDoSummary value, TodoTreeBuilder builder) {
    super(project, value, builder);
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public Collection<AbstractTreeNode> getChildren() {
    ArrayList<AbstractTreeNode> children = new ArrayList<>();

    ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(getProject()).getFileIndex();
    if (myToDoSettings.isModulesShown()) {
      for (Iterator i = myBuilder.getAllFiles(); i.hasNext(); ) {
        PsiFile psiFile = (PsiFile)i.next();
        if (psiFile == null) { // skip invalid PSI files
          continue;
        }
        VirtualFile virtualFile = psiFile.getVirtualFile();
        createModuleTodoNodeForFile(children, projectFileIndex, virtualFile);
      }
    }
    else {
      if (myToDoSettings.getIsPackagesShown()) {
        if (myBuilder instanceof CurrentFileTodosTreeBuilder) {
          Iterator allFiles = myBuilder.getAllFiles();
          if (allFiles.hasNext()) {
            children.add(new TodoFileNode(myProject, (PsiFile)allFiles.next(), myBuilder, false));
          }
        }
        else {
          TodoTreeHelper.addPackagesToChildren(children, getProject(), null, myBuilder);
        }
      }
      else {
        for (Iterator i = myBuilder.getAllFiles(); i.hasNext(); ) {
          PsiFile psiFile = (PsiFile)i.next();
          if (psiFile == null) { // skip invalid PSI files
            continue;
          }
          TodoFileNode fileNode = new TodoFileNode(getProject(), psiFile, myBuilder, false);
          if (getTreeStructure().accept(psiFile) && !children.contains(fileNode)) {
            children.add(fileNode);
          }
        }
      }
    }
    Collections.sort(children, TodoFileDirAndModuleComparator.INSTANCE);
    return children;

  }

  protected void createModuleTodoNodeForFile(ArrayList<? super AbstractTreeNode> children, ProjectFileIndex projectFileIndex, VirtualFile virtualFile) {
    Module module = projectFileIndex.getModuleForFile(virtualFile);
    if (module != null) {
      ModuleToDoNode moduleToDoNode = new ModuleToDoNode(getProject(), module, myBuilder);
      if (!children.contains(moduleToDoNode)) {
        children.add(moduleToDoNode);
      }
    }
  }

  @Override
  public void update(@Nonnull PresentationData presentation) {
    if (DumbService.getInstance(getProject()).isDumb()) return;
    int todoItemCount = getTodoItemCount(getValue());
    int fileCount = getFileCount(getValue());
    presentation.setPresentableText(IdeLocalize.nodeTodoSummary(todoItemCount, fileCount));
  }

  @Override
  public String getTestPresentation() {
    return "Summary";
  }

  @Override
  public int getFileCount(ToDoSummary summary) {
    int count = 0;
    for (Iterator i = myBuilder.getAllFiles(); i.hasNext(); ) {
      PsiFile psiFile = (PsiFile)i.next();
      if (psiFile == null) { // skip invalid PSI files
        continue;
      }
      if (getTreeStructure().accept(psiFile)) {
        count++;
      }
    }
    return count;
  }

  @Override
  public int getTodoItemCount(ToDoSummary val) {
    int count = 0;
    for (Iterator<PsiFile> i = myBuilder.getAllFiles(); i.hasNext(); ) {
      count += ReadAction.compute(() -> getTreeStructure().getTodoItemCount(i.next()));
    }
    return count;
  }

  @Override
  public int getWeight() {
    return 0;
  }
}

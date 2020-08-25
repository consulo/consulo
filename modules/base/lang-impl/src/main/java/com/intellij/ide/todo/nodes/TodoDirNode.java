// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.todo.nodes;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.todo.TodoTreeBuilder;
import com.intellij.ide.todo.TodoTreeStructure;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import consulo.ide.IconDescriptorUpdaters;
import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.Iterator;

public final class TodoDirNode extends PsiDirectoryNode {
  private final TodoTreeBuilder myBuilder;


  public TodoDirNode(Project project, @Nonnull PsiDirectory directory, TodoTreeBuilder builder) {
    super(project, directory, ViewSettings.DEFAULT);
    myBuilder = builder;
  }

  @Override
  protected void updateImpl(@Nonnull PresentationData data) {
    super.updateImpl(data);
    int fileCount = getFileCount(getValue());
    if (getValue() == null || !getValue().isValid() || fileCount == 0) {
      setValue(null);
      return;
    }

    VirtualFile directory = getValue().getVirtualFile();
    boolean isProjectRoot = !ProjectRootManager.getInstance(getProject()).getFileIndex().isInContent(directory);
    String newName = isProjectRoot || getStructure().getIsFlattenPackages() ? getValue().getVirtualFile().getPresentableUrl() : getValue().getName();

    int todoItemCount = getTodoItemCount(getValue());
    data.setLocationString(IdeBundle.message("node.todo.group", todoItemCount));
    data.setPresentableText(newName);
  }

  private TodoTreeStructure getStructure() {
    return myBuilder.getTodoTreeStructure();
  }

  @Override
  public Collection<AbstractTreeNode> getChildrenImpl() {
    return TodoTreeHelper.getDirectoryChildren(getValue(), myBuilder, getSettings().isFlattenPackages());
  }

  public int getFileCount(PsiDirectory directory) {
    Iterator<PsiFile> iterator = myBuilder.getFiles(directory);
    int count = 0;
    try {
      while (iterator.hasNext()) {
        PsiFile psiFile = iterator.next();
        if (getStructure().accept(psiFile)) {
          count++;
        }
      }
    }
    catch (IndexNotReadyException e) {
      return count;
    }
    return count;
  }

  public int getTodoItemCount(PsiDirectory directory) {
    if (TodoTreeHelper.skipDirectory(directory)) {
      return 0;
    }
    int count = 0;
    Iterator<PsiFile> iterator = myBuilder.getFiles(directory);
    while (iterator.hasNext()) {
      PsiFile psiFile = iterator.next();
      count += getStructure().getTodoItemCount(psiFile);
    }
    return count;
  }

  @Override
  public int getWeight() {
    return 2;
  }


}

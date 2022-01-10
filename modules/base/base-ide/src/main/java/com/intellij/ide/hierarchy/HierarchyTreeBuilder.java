/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.ide.hierarchy;

import com.intellij.ide.util.treeView.AbstractTreeBuilder;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.ide.util.treeView.TreeBuilderUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.StatusBarProgress;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.FileStatusListener;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import javax.annotation.Nonnull;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HierarchyTreeBuilder extends AbstractTreeBuilder {
  HierarchyTreeBuilder(@Nonnull Project project,
                       final JTree tree,
                       final DefaultTreeModel treeModel,
                       final HierarchyTreeStructure treeStructure,
                       final Comparator<NodeDescriptor> comparator) {
    super(tree, treeModel, treeStructure, comparator);

    initRootNode();
    PsiManager.getInstance(project).addPsiTreeChangeListener(new MyPsiTreeChangeListener(), this);
    FileStatusManager.getInstance(project).addFileStatusListener(new MyFileStatusListener(), this);
  }

  @Nonnull
  public Pair<List<Object>, List<Object>> storeExpandedAndSelectedInfo() {
    List<Object> pathsToExpand = new ArrayList<Object>();
    List<Object> selectionPaths = new ArrayList<Object>();
    TreeBuilderUtil.storePaths(this, getRootNode(), pathsToExpand, selectionPaths, true);
    return Pair.create(pathsToExpand, selectionPaths);
  }

  public final void restoreExpandedAndSelectedInfo(@Nonnull Pair<List<Object>, List<Object>> pair) {
    TreeBuilderUtil.restorePaths(this, pair.first, pair.second, true);
  }

  @Override
  protected boolean isAlwaysShowPlus(final NodeDescriptor nodeDescriptor) {
    return ((HierarchyTreeStructure) getTreeStructure()).isAlwaysShowPlus();
  }

  @Override
  protected boolean isAutoExpandNode(final NodeDescriptor nodeDescriptor) {
    return getTreeStructure().getRootElement().equals(nodeDescriptor.getElement())
           || !(nodeDescriptor instanceof HierarchyNodeDescriptor);
  }

  @Override
  protected final boolean isSmartExpand() {
    return false;
  }

  @Override
  protected final boolean isDisposeOnCollapsing(final NodeDescriptor nodeDescriptor) {
    return false; // prevents problems with building descriptors for invalidated elements
  }


  @Override
  @Nonnull
  protected ProgressIndicator createProgressIndicator() {
    return new StatusBarProgress();
  }

  private final class MyPsiTreeChangeListener extends PsiTreeChangeAdapter {
    @Override
    public final void childAdded(@Nonnull final PsiTreeChangeEvent event) {
      getUpdater().addSubtreeToUpdate(getRootNode());
    }

    @Override
    public final void childRemoved(@Nonnull final PsiTreeChangeEvent event) {
      getUpdater().addSubtreeToUpdate(getRootNode());
    }

    @Override
    public final void childReplaced(@Nonnull final PsiTreeChangeEvent event) {
      getUpdater().addSubtreeToUpdate(getRootNode());
    }

    @Override
    public final void childMoved(@Nonnull final PsiTreeChangeEvent event) {
      getUpdater().addSubtreeToUpdate(getRootNode());
    }

    @Override
    public final void childrenChanged(@Nonnull final PsiTreeChangeEvent event) {
      getUpdater().addSubtreeToUpdate(getRootNode());
    }

    @Override
    public final void propertyChanged(@Nonnull final PsiTreeChangeEvent event) {
      getUpdater().addSubtreeToUpdate(getRootNode());
    }
  }

  private final class MyFileStatusListener implements FileStatusListener {
    @Override
    public final void fileStatusesChanged() {
      getUpdater().addSubtreeToUpdate(getRootNode());
    }

    @Override
    public final void fileStatusChanged(@Nonnull final VirtualFile virtualFile) {
      getUpdater().addSubtreeToUpdate(getRootNode());
    }
  }
}

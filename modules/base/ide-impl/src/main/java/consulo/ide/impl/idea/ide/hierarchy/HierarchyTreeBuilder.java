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

package consulo.ide.impl.idea.ide.hierarchy;

import consulo.language.psi.PsiManager;
import consulo.language.psi.event.PsiTreeChangeAdapter;
import consulo.language.psi.event.PsiTreeChangeEvent;
import consulo.project.Project;
import consulo.ui.ex.awt.tree.AbstractTreeBuilder;
import consulo.ui.ex.awt.tree.TreeBuilderUtil;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.util.lang.Couple;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatusListener;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HierarchyTreeBuilder extends AbstractTreeBuilder {
    HierarchyTreeBuilder(
        @Nonnull Project project,
        JTree tree,
        DefaultTreeModel treeModel,
        HierarchyTreeStructure treeStructure,
        Comparator<NodeDescriptor> comparator
    ) {
        super(tree, treeModel, treeStructure, comparator);

        initRootNode();
        PsiManager.getInstance(project).addPsiTreeChangeListener(new MyPsiTreeChangeListener(), this);
        FileStatusManager.getInstance(project).addFileStatusListener(new MyFileStatusListener(), this);
    }

    @Nonnull
    public Couple<List<Object>> storeExpandedAndSelectedInfo() {
        List<Object> pathsToExpand = new ArrayList<>();
        List<Object> selectionPaths = new ArrayList<>();
        TreeBuilderUtil.storePaths(this, getRootNode(), pathsToExpand, selectionPaths, true);
        return Couple.of(pathsToExpand, selectionPaths);
    }

    public final void restoreExpandedAndSelectedInfo(@Nonnull Couple<List<Object>> pair) {
        TreeBuilderUtil.restorePaths(this, pair.first, pair.second, true);
    }

    @Override
    protected boolean isAlwaysShowPlus(NodeDescriptor nodeDescriptor) {
        return ((HierarchyTreeStructure)getTreeStructure()).isAlwaysShowPlus();
    }

    @Override
    protected boolean isAutoExpandNode(NodeDescriptor nodeDescriptor) {
        return getTreeStructure().getRootElement().equals(nodeDescriptor.getElement())
            || !(nodeDescriptor instanceof HierarchyNodeDescriptor);
    }

    @Override
    protected final boolean isSmartExpand() {
        return false;
    }

    @Override
    protected final boolean isDisposeOnCollapsing(NodeDescriptor nodeDescriptor) {
        return false; // prevents problems with building descriptors for invalidated elements
    }

    private final class MyPsiTreeChangeListener extends PsiTreeChangeAdapter {
        @Override
        public final void childAdded(@Nonnull PsiTreeChangeEvent event) {
            getUpdater().addSubtreeToUpdate(getRootNode());
        }

        @Override
        public final void childRemoved(@Nonnull PsiTreeChangeEvent event) {
            getUpdater().addSubtreeToUpdate(getRootNode());
        }

        @Override
        public final void childReplaced(@Nonnull PsiTreeChangeEvent event) {
            getUpdater().addSubtreeToUpdate(getRootNode());
        }

        @Override
        public final void childMoved(@Nonnull PsiTreeChangeEvent event) {
            getUpdater().addSubtreeToUpdate(getRootNode());
        }

        @Override
        public final void childrenChanged(@Nonnull PsiTreeChangeEvent event) {
            getUpdater().addSubtreeToUpdate(getRootNode());
        }

        @Override
        public final void propertyChanged(@Nonnull PsiTreeChangeEvent event) {
            getUpdater().addSubtreeToUpdate(getRootNode());
        }
    }

    private final class MyFileStatusListener implements FileStatusListener {
        @Override
        public final void fileStatusesChanged() {
            getUpdater().addSubtreeToUpdate(getRootNode());
        }

        @Override
        public final void fileStatusChanged(@Nonnull VirtualFile virtualFile) {
            getUpdater().addSubtreeToUpdate(getRootNode());
        }
    }
}

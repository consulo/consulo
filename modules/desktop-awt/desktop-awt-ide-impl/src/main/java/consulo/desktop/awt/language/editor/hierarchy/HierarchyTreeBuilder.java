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

package consulo.desktop.awt.language.editor.hierarchy;

import consulo.ui.ex.awt.tree.AbstractTreeBuilder;
import consulo.ui.ex.awt.tree.TreeBuilderUtil;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.util.lang.Couple;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import consulo.language.editor.hierarchy.HierarchyTreeStructure;
import consulo.language.editor.hierarchy.HierarchyNodeDescriptor;

public class HierarchyTreeBuilder extends AbstractTreeBuilder {
    HierarchyTreeBuilder(
        JTree tree,
        DefaultTreeModel treeModel,
        HierarchyTreeStructure treeStructure,
        Comparator<NodeDescriptor> comparator
    ) {
        super(tree, treeModel, treeStructure, comparator);

        initRootNode();
    }

    public void invalidate() {
        getUpdater().addSubtreeToUpdate(getRootNode());
    }

    
    public Couple<List<Object>> storeExpandedAndSelectedInfo() {
        List<Object> pathsToExpand = new ArrayList<>();
        List<Object> selectionPaths = new ArrayList<>();
        TreeBuilderUtil.storePaths(this, getRootNode(), pathsToExpand, selectionPaths, true);
        return Couple.of(pathsToExpand, selectionPaths);
    }

    public final void restoreExpandedAndSelectedInfo(Couple<List<Object>> pair) {
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
}

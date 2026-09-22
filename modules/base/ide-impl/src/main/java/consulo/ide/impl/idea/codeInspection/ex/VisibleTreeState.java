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
package consulo.ide.impl.idea.codeInspection.ex;

import consulo.ide.impl.idea.profile.codeInspection.ui.inspectionsTree.InspectionConfigTreeNode;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.util.lang.Comparing;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Tag;
import org.jspecify.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.*;

/**
 * @author anna
 * @since 2004-12-18
 */
@Tag("profile-state")
public class VisibleTreeState {
    @Tag("expanded-state")
    @AbstractCollection(surroundWithTag = false, elementTag = "expanded", elementValueAttribute = "path", elementTypes = {State.class})
    public TreeSet<State> myExpandedNodes = new TreeSet<>();

    @Tag("selected-state")
    @AbstractCollection(surroundWithTag = false, elementTag = "selected", elementValueAttribute = "path", elementTypes = {State.class})
    public TreeSet<State> mySelectedNodes = new TreeSet<>();

    public VisibleTreeState(VisibleTreeState src) {
        myExpandedNodes.addAll(src.myExpandedNodes);
        mySelectedNodes.addAll(src.mySelectedNodes);
    }

    public VisibleTreeState() {
    }

    public void expandNode(InspectionConfigTreeNode node) {
        myExpandedNodes.add(getState(node));
    }

    public void collapseNode(InspectionConfigTreeNode node) {
        myExpandedNodes.remove(getState(node));
    }

    public void restoreVisibleState(Tree tree) {
        List<TreePath> pathsToExpand = new ArrayList<>();
        List<TreePath> toSelect = new ArrayList<>();
        traverseNodes((DefaultMutableTreeNode) tree.getModel().getRoot(), pathsToExpand, toSelect);
        TreeUtil.restoreExpandedPaths(tree, pathsToExpand);
        if (toSelect.isEmpty()) {
            TreeUtil.selectFirstNode(tree);
        }
        else {
            for (TreePath aToSelect : toSelect) {
                TreeUtil.selectPath(tree, aToSelect);
            }
        }
    }

    private void traverseNodes(DefaultMutableTreeNode root, List<TreePath> pathsToExpand, List<TreePath> toSelect) {
        State state = getState((InspectionConfigTreeNode) root);
        TreeNode[] rootPath = root.getPath();
        if (mySelectedNodes.contains(state)) {
            toSelect.add(new TreePath(rootPath));
        }
        if (myExpandedNodes.contains(state)) {
            pathsToExpand.add(new TreePath(rootPath));
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            traverseNodes((DefaultMutableTreeNode) root.getChildAt(i), pathsToExpand, toSelect);
        }
    }

    public void saveVisibleState(Tree tree) {
        myExpandedNodes.clear();
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) tree.getModel().getRoot();
        Enumeration<TreePath> expanded = tree.getExpandedDescendants(new TreePath(rootNode.getPath()));
        if (expanded != null) {
            while (expanded.hasMoreElements()) {
                TreePath treePath = expanded.nextElement();
                InspectionConfigTreeNode node = (InspectionConfigTreeNode) treePath.getLastPathComponent();
                myExpandedNodes.add(getState(node));
            }
        }

        setSelectionPaths(tree.getSelectionPaths());
    }

    private static State getState(InspectionConfigTreeNode node) {
        Descriptor descriptor = node.getDefaultDescriptor();
        State expandedNode;
        if (descriptor != null) {
            expandedNode = new State(descriptor);
        }
        else {
            StringBuilder buf = new StringBuilder();
            while (node.getParent() != null) {
                buf.append(node.getGroupName());
                node = (InspectionConfigTreeNode) node.getParent();
            }
            expandedNode = new State(buf.toString());
        }
        return expandedNode;
    }

    public void setSelectionPaths(TreePath[] selectionPaths) {
        mySelectedNodes.clear();
        if (selectionPaths != null) {
            for (TreePath selectionPath : selectionPaths) {
                InspectionConfigTreeNode node = (InspectionConfigTreeNode) selectionPath.getLastPathComponent();
                mySelectedNodes.add(getState(node));
            }
        }
    }

    public static class State implements Comparable<State> {
        @Tag("id")
        public String myKey;
        Descriptor myDescriptor;

        public State(String key) {
            myKey = key;
        }

        public State(Descriptor descriptor) {
            myKey = descriptor.toString();
            myDescriptor = descriptor;
        }

        //readExternal
        public State() {
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            State that = (State) o;

            return Objects.equals(myKey, that.myKey);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(myKey);
        }

        @Override
        public int compareTo(State other) {
            int result = Comparing.compare(myKey, other.myKey);
            if (result != 0) {
                return result;
            }
            if (myDescriptor != null && other.myDescriptor != null) {
                String scope1 = myDescriptor.getScopeName();
                String scope2 = other.myDescriptor.getScopeName();
                return scope1.compareTo(scope2);
            }
            return 0;
        }
    }
}

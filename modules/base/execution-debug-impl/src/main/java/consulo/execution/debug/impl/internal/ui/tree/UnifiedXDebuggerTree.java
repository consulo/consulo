/*
 * Copyright 2013-2026 consulo.io
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
package consulo.execution.debug.impl.internal.ui.tree;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.frame.XValueNode;
import consulo.execution.debug.impl.internal.ui.tree.node.UnifiedXDebuggerTreeNode;
import consulo.execution.debug.impl.internal.ui.tree.node.UnifiedXValueContainerNode;
import consulo.execution.debug.impl.internal.ui.tree.node.UnifiedXValueNodeImpl;
import consulo.execution.debug.ui.XValueTree;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tree.ApplicationTreeExecutorFactory;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * The tree of the unified debugger views - the counterpart of the swing {@link XDebuggerTree}. The structure is the one
 * of the root node set last: a level is built by its node on a background thread of the tree, the way the swing nodes
 * are filled by the debug process, and a node which changes later refreshes its row.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
public class UnifiedXDebuggerTree implements XValueTree {
    private final Project myProject;
    private final @Nullable XDebugSession mySession;
    private final UIAccess myUIAccess;
    private final Tree<UnifiedXDebuggerTreeNode> myTree;

    private volatile @Nullable UnifiedXValueContainerNode<?> myRoot;
    /**
     * The build of the root set last - what is opened again waits for it.
     */
    private CompletableFuture<?> myRootBuilt = CompletableFuture.completedFuture(null);

    @RequiredUIAccess
    public UnifiedXDebuggerTree(Project project, @Nullable XDebugSession session, Disposable parent) {
        myProject = project;
        mySession = session;
        myUIAccess = project.getUIAccess();

        ApplicationTreeExecutorFactory executorFactory = Application.get().getInstance(ApplicationTreeExecutorFactory.class);
        // a level waits for the debug process, which must not happen with the read lock taken
        myTree = Tree.create(new UnifiedXDebuggerTreeModel(), executorFactory.forBackgroundThreadWithoutReadAction(parent));
        Disposer.register(parent, myTree.destroyHook());
    }

    public Component getComponent() {
        return myTree;
    }

    public UIAccess getUIAccess() {
        return myUIAccess;
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public @Nullable XDebugSession getSession() {
        return mySession;
    }

    @RequiredUIAccess
    public void setRoot(UnifiedXValueContainerNode<?> root) {
        myRoot = root;
        myRootBuilt = myTree.refreshAll();
    }

    public @Nullable UnifiedXValueContainerNode<?> getRoot() {
        return myRoot;
    }

    public void markNodesObsolete() {
        UnifiedXValueContainerNode<?> root = myRoot;
        if (root != null) {
            root.setObsolete();
        }
    }

    /**
     * Draws the row of the node again - its presentation arrived after the level was shown.
     */
    public void nodeChanged(UnifiedXDebuggerTreeNode node) {
        myUIAccess.give(() -> {
            TreeNode<UnifiedXDebuggerTreeNode> treeNode = node.getTreeNode();
            if (treeNode != null) {
                treeNode.setLeaf(node.isLeaf());
                myTree.refreshItem(treeNode, false);
            }
        });
    }

    /**
     * The children of the node changed - a watch was added or removed. The level is built again, and what was open
     * below the root is opened again, the way the swing tree keeps its expanded nodes when nodes are inserted.
     */
    @RequiredUIAccess
    public void nodeStructureChanged(UnifiedXValueContainerNode<?> node) {
        if (node == myRoot) {
            List<List<String>> state = saveState();
            setRoot(node);
            restoreState(state);
            return;
        }

        TreeNode<UnifiedXDebuggerTreeNode> treeNode = node.getTreeNode();
        if (treeNode != null) {
            myTree.refreshItem(treeNode, true);
        }
    }

    public void addSelectionListener(Runnable listener) {
        myTree.addSelectListener(event -> listener.run());
    }

    public <T> List<T> getSelectedNodes(Class<T> nodeClass) {
        List<T> result = new ArrayList<>();
        for (TreeNode<UnifiedXDebuggerTreeNode> treeNode : myTree.getSelectedNodes()) {
            UnifiedXDebuggerTreeNode node = treeNode.getValue();
            if (nodeClass.isInstance(node)) {
                result.add(nodeClass.cast(node));
            }
        }
        return result;
    }

    @Override
    public List<XValueNode> getSelectedNodes() {
        return new ArrayList<>(getSelectedNodes(UnifiedXValueNodeImpl.class));
    }

    @Override
    public @Nullable XValueNode getSelectedNode() {
        List<XValueNode> nodes = getSelectedNodes();
        return nodes.isEmpty() ? null : nodes.get(0);
    }

    @Override
    public void expand(XValueNode node) {
        if (!(node instanceof UnifiedXValueNodeImpl impl)) {
            throw new IllegalArgumentException("Not supported implementation: " + node.getClass());
        }

        TreeNode<UnifiedXDebuggerTreeNode> treeNode = impl.getTreeNode();
        if (treeNode != null) {
            myUIAccess.giveIfNeed(() -> myTree.expand(treeNode));
        }
    }

    /**
     * What is open, by the names of the nodes down to it - the nodes of a rebuilt tree are new, so the swing
     * {@code XDebuggerTreeState} remembers them the same way.
     */
    @RequiredUIAccess
    public List<List<String>> saveState() {
        List<List<String>> state = new ArrayList<>();
        for (List<TreeNode<UnifiedXDebuggerTreeNode>> path : myTree.getExpandedPaths()) {
            List<String> names = new ArrayList<>(path.size());
            for (TreeNode<UnifiedXDebuggerTreeNode> treeNode : path) {
                UnifiedXDebuggerTreeNode node = treeNode.getValue();
                String name = node == null ? null : node.getRestorableName();
                if (name == null) {
                    names = null;
                    break;
                }
                names.add(name);
            }

            if (names != null && !names.isEmpty()) {
                state.add(names);
            }
        }
        return state;
    }

    /**
     * Opens again what was open - as long as the root it was started for is still shown.
     */
    @RequiredUIAccess
    public void restoreState(List<List<String>> state) {
        UnifiedXValueContainerNode<?> root = myRoot;
        myRootBuilt.thenRun(() -> myUIAccess.give(() -> {
            TreeNode<UnifiedXDebuggerTreeNode> rootNode = myTree.getRootNode();
            if (rootNode == null) {
                return;
            }
            for (List<String> path : state) {
                restorePath(rootNode, path, 0, root);
            }
        }));
    }

    private void restorePath(TreeNode<UnifiedXDebuggerTreeNode> parent, List<String> path, int index, @Nullable UnifiedXValueContainerNode<?> root) {
        if (index >= path.size() || root != myRoot) {
            return;
        }

        String name = path.get(index);
        parent.findChild(node -> name.equals(node.getRestorableName())).thenAccept(child -> {
            if (child == null) {
                return;
            }

            myUIAccess.give(() -> {
                if (root == myRoot) {
                    myTree.expand(child).thenRun(() -> restorePath(child, path, index + 1, root));
                }
            });
        });
    }

    private class UnifiedXDebuggerTreeModel implements TreeModel<UnifiedXDebuggerTreeNode> {
        @Override
        public void buildChildren(
            Function<UnifiedXDebuggerTreeNode, TreeNode<UnifiedXDebuggerTreeNode>> nodeFactory,
            @Nullable UnifiedXDebuggerTreeNode parentValue
        ) {
            UnifiedXDebuggerTreeNode parent = parentValue == null ? myRoot : parentValue;
            if (!(parent instanceof UnifiedXValueContainerNode<?> container)) {
                return;
            }

            for (UnifiedXDebuggerTreeNode child : container.loadChildren()) {
                TreeNode<UnifiedXDebuggerTreeNode> treeNode = nodeFactory.apply(child);
                treeNode.setLeaf(child.isLeaf());
                treeNode.setRenderer(UnifiedXDebuggerTreeNode::render);
                child.setTreeNode(treeNode);
            }
        }
    }
}

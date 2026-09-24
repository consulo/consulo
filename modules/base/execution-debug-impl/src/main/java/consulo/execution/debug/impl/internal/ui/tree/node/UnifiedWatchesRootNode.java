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
package consulo.execution.debug.impl.internal.ui.tree.node;

import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.frame.XCompositeNode;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debug.frame.XValueChildrenList;
import consulo.execution.debug.frame.XValueContainer;
import consulo.execution.debug.impl.internal.frame.XWatchesView;
import consulo.execution.debug.impl.internal.ui.tree.UnifiedXDebuggerTree;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The watches, followed by the variables of the frame when they are shown together - the counterpart of the swing
 * {@link WatchesRootNode}.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
public class UnifiedWatchesRootNode extends UnifiedXValueContainerNode<XValueContainer> {
    private final XWatchesView myWatchesView;
    private final List<UnifiedWatchNodeImpl> myChildren;

    public UnifiedWatchesRootNode(
        UnifiedXDebuggerTree tree,
        XWatchesView watchesView,
        XExpression[] expressions,
        @Nullable XStackFrame stackFrame,
        boolean watchesInVariables
    ) {
        super(tree, null, new XValueContainer() {
            @Override
            public void computeChildren(XCompositeNode node) {
                if (stackFrame != null && watchesInVariables) {
                    stackFrame.computeChildren(node);
                }
                else {
                    node.addChildren(XValueChildrenList.EMPTY, true);
                }
            }
        });
        setLeaf(false);
        myWatchesView = watchesView;
        myChildren = new ArrayList<>();

        // copy evaluation result by default
        if (stackFrame != null && tree.getRoot() instanceof UnifiedWatchesRootNode watchesRootNode) {
            for (UnifiedWatchNodeImpl node : watchesRootNode.getWatchChildren()) {
                if (node instanceof UnifiedResultNode) {
                    myChildren.add(new UnifiedResultNode(tree, this, node.getExpression(), node.getValueContainer()));
                    break;
                }
            }
        }

        for (XExpression watchExpression : expressions) {
            myChildren.add(new UnifiedWatchNodeImpl(tree, this, watchExpression, stackFrame));
        }
    }

    @RequiredUIAccess
    public void addResultNode(@Nullable XStackFrame stackFrame, XExpression expression) {
        UnifiedWatchNodeImpl message = new UnifiedResultNode(myTree, this, expression, stackFrame);
        synchronized (this) {
            if (!myChildren.isEmpty() && myChildren.get(0) instanceof UnifiedResultNode) {
                myChildren.set(0, message);
            }
            else {
                myChildren.add(0, message);
            }
        }
        myTree.nodeStructureChanged(this);
    }

    public synchronized List<XExpression> getWatchExpressions() {
        List<XExpression> expressions = new ArrayList<>(myChildren.size());
        for (UnifiedWatchNodeImpl child : myChildren) {
            if (!(child instanceof UnifiedResultNode)) {
                expressions.add(child.getExpression());
            }
        }
        return expressions;
    }

    public synchronized void removeResultNode() {
        myChildren.removeIf(node -> node instanceof UnifiedResultNode);
    }

    public synchronized List<UnifiedWatchNodeImpl> getWatchChildren() {
        return List.copyOf(myChildren);
    }

    @Override
    public synchronized List<UnifiedXDebuggerTreeNode> getChildren() {
        List<UnifiedXDebuggerTreeNode> children = new ArrayList<>(myChildren);
        children.addAll(super.getChildren());
        return children;
    }

    @Override
    public synchronized List<UnifiedXValueContainerNode<?>> getLoadedChildren() {
        List<UnifiedXValueContainerNode<?>> children = new ArrayList<>(myChildren);
        children.addAll(super.getLoadedChildren());
        return children;
    }

    public void computeWatches() {
        getWatchChildren().forEach(UnifiedWatchNodeImpl::computePresentationIfNeeded);
    }

    @RequiredUIAccess
    public void addWatchExpression(@Nullable XStackFrame stackFrame, XExpression expression, int index, boolean navigateToWatchNode) {
        UnifiedWatchNodeImpl message = new UnifiedWatchNodeImpl(myTree, this, expression, stackFrame);
        synchronized (this) {
            if (index == -1) {
                myChildren.add(message);
            }
            else {
                myChildren.add(index, message);
            }
        }
        myTree.nodeStructureChanged(this);
    }

    @RequiredUIAccess
    public void removeChildren(Collection<? extends UnifiedWatchNodeImpl> nodes) {
        synchronized (this) {
            myChildren.removeAll(nodes);
        }
        myTree.nodeStructureChanged(this);
    }

    @RequiredUIAccess
    public void removeAllChildren() {
        synchronized (this) {
            myChildren.clear();
        }
        myTree.nodeStructureChanged(this);
    }
}

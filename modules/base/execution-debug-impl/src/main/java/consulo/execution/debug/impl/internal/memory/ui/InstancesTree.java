// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.impl.internal.memory.ui;

import consulo.execution.debug.XDebuggerActions;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.frame.*;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTree;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTreeState;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueNodeImpl;
import consulo.execution.debug.ui.XDebuggerUIConstants;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class InstancesTree extends XDebuggerTree {
    private final XValueNodeImpl myRoot;
    private final Runnable myOnRootExpandAction;
    private List<XValueChildrenList> myChildren;

    public InstancesTree(@Nonnull Project project,
                         @Nonnull XDebuggerEditorsProvider editorsProvider,
                         @Nullable XValueMarkers<?, ?> valueMarkers,
                         @Nonnull Runnable onRootExpand) {
        super(project, editorsProvider, null, XDebuggerActions.INSPECT_TREE_POPUP_GROUP, valueMarkers);
        myOnRootExpandAction = onRootExpand;
        myRoot = new XValueNodeImpl(this, null, "root", new MyRootValue());

        myRoot.children();
        setRoot(myRoot, false);
        myRoot.setLeaf(false);
        setSelectionRow(0);
        expandNodesOnLoad(node -> node == myRoot);
    }

    public void addChildren(@Nonnull XValueChildrenList children, boolean last) {
        if (myChildren == null) {
            myChildren = new ArrayList<>();
        }

        myChildren.add(children);
        myRoot.addChildren(children, last);
    }

    void rebuildTree(@Nonnull RebuildPolicy policy, @Nonnull XDebuggerTreeState state) {
        if (policy == RebuildPolicy.RELOAD_INSTANCES) {
            myChildren = null;
        }

        rebuildAndRestore(state);
    }

    public void rebuildTree(@Nonnull RebuildPolicy policy) {
        rebuildTree(policy, XDebuggerTreeState.saveState(this));
    }

    void setInfoMessage(@SuppressWarnings("SameParameterValue") @Nonnull String text) {
        myChildren = null;
        myRoot.clearChildren();
        myRoot.setMessage(text, XDebuggerUIConstants.INFORMATION_MESSAGE_ICON, SimpleTextAttributes.REGULAR_ATTRIBUTES, null);
    }


    public enum RebuildPolicy {
        RELOAD_INSTANCES,
        ONLY_UPDATE_LABELS
    }

    private class MyRootValue extends XValue {
        @Override
        public void computeChildren(@Nonnull XCompositeNode node) {
            if (myChildren == null) {
                myOnRootExpandAction.run();
            }
            else {
                for (XValueChildrenList children : myChildren) {
                    myRoot.addChildren(children, false);
                }

                myRoot.addChildren(XValueChildrenList.EMPTY, true);
            }
        }

        @Override
        public void computePresentation(@Nonnull XValueNode node, @Nonnull XValuePlace place) {
            node.setPresentation(null, "", "", true);
        }
    }
}

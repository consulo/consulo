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
package consulo.execution.debug.impl.internal.frame;

import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debug.frame.XValueContainer;
import consulo.execution.debug.impl.internal.ui.tree.UnifiedXDebuggerTree;
import consulo.execution.debug.impl.internal.ui.tree.node.UnifiedXStackFrameNode;
import consulo.execution.debug.impl.internal.ui.tree.node.UnifiedXValueContainerNode;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The counterpart of the swing {@link XVariablesViewBase}, built of unified components.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
public abstract class UnifiedXVariablesViewBase extends XDebugView {
    private final UnifiedXDebuggerTree myTree;
    private @Nullable List<List<String>> myTreeState;
    private @Nullable Object myFrameEqualityObject;

    @RequiredUIAccess
    protected UnifiedXVariablesViewBase(Project project, @Nullable XDebugSession session) {
        myTree = new UnifiedXDebuggerTree(project, session, this);
    }

    @RequiredUIAccess
    protected void buildTreeAndRestoreState(XStackFrame stackFrame) {
        createNewRootNode(stackFrame);
        Object newEqualityObject = stackFrame.getEqualityObject();
        if (myFrameEqualityObject != null && newEqualityObject != null && myFrameEqualityObject.equals(newEqualityObject) && myTreeState != null) {
            myTree.restoreState(myTreeState);
        }
    }

    @RequiredUIAccess
    protected UnifiedXValueContainerNode<?> createNewRootNode(@Nullable XStackFrame stackFrame) {
        UnifiedXValueContainerNode<?> root;
        if (stackFrame == null) {
            root = new UnifiedXValueContainerNode<>(myTree, null, new XValueContainer() {
            }) {
            };
        }
        else {
            root = new UnifiedXStackFrameNode(myTree, stackFrame);
        }
        myTree.setRoot(root);
        return root;
    }

    @RequiredUIAccess
    protected void saveCurrentTreeState(@Nullable XStackFrame stackFrame) {
        myFrameEqualityObject = stackFrame != null ? stackFrame.getEqualityObject() : null;
        myTreeState = myTree.saveState();
    }

    @Override
    protected void clear() {
    }

    public final UnifiedXDebuggerTree getTree() {
        return myTree;
    }

    public Component getPanel() {
        return myTree.getComponent();
    }

    @Override
    public void dispose() {
    }
}

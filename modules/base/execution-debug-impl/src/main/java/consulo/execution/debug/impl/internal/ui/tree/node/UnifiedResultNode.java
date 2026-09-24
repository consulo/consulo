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

import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debug.frame.XValue;
import consulo.execution.debug.frame.presentation.XValuePresentation;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.debug.impl.internal.ui.tree.UnifiedXDebuggerTree;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * The result of the expression evaluated from the field of the watches - the counterpart of the swing
 * {@link ResultNode}.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
public class UnifiedResultNode extends UnifiedWatchNodeImpl {
    UnifiedResultNode(UnifiedXDebuggerTree tree, UnifiedWatchesRootNode parent, XExpression expression, @Nullable XStackFrame stackFrame) {
        super(tree, parent, expression, stackFrame, XDebuggerLocalize.debuggerResultNodeName());
    }

    UnifiedResultNode(UnifiedXDebuggerTree tree, UnifiedWatchesRootNode parent, XExpression expression, XValue value) {
        super(tree, parent, expression, XDebuggerLocalize.debuggerResultNodeName(), value);
    }

    @Override
    public void applyPresentation(@Nullable Image icon, XValuePresentation valuePresentation, boolean hasChildren) {
        super.applyPresentation(ExecutionDebugIconGroup.actionEvaluateexpression(), valuePresentation, hasChildren);
    }

    @Override
    public @Nullable String getRestorableName() {
        return null;
    }

    @Override
    protected void evaluated() {
        // the expression could change what the variables hold
        myTree.getUIAccess().give(() -> {
            XDebugSession session = myTree.getSession();
            if (session != null) {
                session.rebuildViews();
            }
        });
    }
}

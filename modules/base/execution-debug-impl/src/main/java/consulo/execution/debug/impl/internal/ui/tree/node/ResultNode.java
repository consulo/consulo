/*
 * Copyright 2013-2025 consulo.io
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

import consulo.application.ApplicationManager;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerBundle;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debug.frame.XValue;
import consulo.execution.debug.frame.presentation.XValuePresentation;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.debug.impl.internal.frame.XDebugView;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTree;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ResultNode extends WatchNodeImpl {
    ResultNode(@Nonnull XDebuggerTree tree,
               @Nonnull WatchesRootNode parent,
               @Nonnull XExpression expression,
               @Nullable XStackFrame stackFrame) {
        super(tree, parent, expression, stackFrame, XDebuggerBundle.message("debugger.result.node.name"));
    }

    ResultNode(@Nonnull XDebuggerTree tree,
               @Nonnull WatchesRootNode parent,
               @Nonnull XExpression expression,
               @Nonnull XValue value) {
        super(tree, parent, expression, XDebuggerBundle.message("debugger.result.node.name"), value);
    }

    @Override
    public void applyPresentation(@Nullable Image icon, @Nonnull XValuePresentation valuePresentation, boolean hasChildren) {
        super.applyPresentation(ExecutionDebugIconGroup.actionEvaluateexpression(), valuePresentation, hasChildren);
    }

    @Override
    protected void evaluated() {
        ApplicationManager.getApplication().invokeLater(() -> {
            XDebugSession session = XDebugView.getSession(getTree());
            if (session != null) {
                session.rebuildViews();
            }
        });
    }
}
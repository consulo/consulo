/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.ui.tree.action;

import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerManager;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.debug.impl.internal.XDebugSessionImpl;
import consulo.execution.debug.impl.internal.action.AddToWatchesAction;
import consulo.execution.debug.impl.internal.frame.XWatchesView;
import consulo.execution.debug.impl.internal.ui.DebuggerUIImplUtil;
import consulo.execution.debug.impl.internal.ui.XDebugSessionTab;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueNodeImpl;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * This action works only in the variables view
 *
 * @see AddToWatchesAction
 */
public class XAddToWatchesTreeAction extends XDebuggerTreeActionBase {
    @Override
    protected boolean isEnabled(@Nonnull final XValueNodeImpl node, @Nonnull AnActionEvent e) {
        return super.isEnabled(node, e) && DebuggerUIImplUtil.hasEvaluationExpression(node.getValueContainer()) && getWatchesView(e) != null;
    }

    @Override
    protected void perform(final XValueNodeImpl node, @Nonnull final String nodeName, final AnActionEvent e) {
        final XWatchesView watchesView = getWatchesView(e);
        if (watchesView != null) {
            node.getValueContainer().calculateEvaluationExpression().doWhenDone(expression -> {
                if (expression != null) {
                    watchesView.addWatchExpression(expression, -1, true);
                }
            });
        }
    }

    private static XWatchesView getWatchesView(@Nonnull AnActionEvent e) {
        XWatchesView view = e.getData(XWatchesView.DATA_KEY);
        Project project = e.getData(Project.KEY);
        if (view == null && project != null) {
            XDebugSession session = XDebuggerManager.getInstance(project).getCurrentSession();
            if (session != null) {
                XDebugSessionTab tab = ((XDebugSessionImpl) session).getSessionTab();
                if (tab != null) {
                    return tab.getWatchesView();
                }
            }
        }
        return view;
    }

    @Nullable
    @Override
    protected Image getTemplateIcon() {
        return ExecutionDebugIconGroup.actionAddtowatch();
    }
}
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

import consulo.execution.debug.XDebugProcess;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debug.impl.internal.XDebugSessionImpl;
import consulo.execution.debug.impl.internal.ui.tree.node.UnifiedXValueContainerNode;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import org.jspecify.annotations.Nullable;

/**
 * The variables of the current stack frame, built of unified components - the counterpart of the swing
 * {@link XVariablesView}.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
public class UnifiedXVariablesView extends UnifiedXVariablesViewBase {
    private final DockLayout myComponent;
    protected final XDebugSessionImpl mySession;

    @RequiredUIAccess
    public UnifiedXVariablesView(XDebugSessionImpl session) {
        super(session.getProject(), session);
        mySession = session;
        myComponent = DockLayout.create();
        myComponent.center(super.getPanel());

        Component topPanel = createTopPanel();
        if (topPanel != null) {
            myComponent.top(topPanel);
        }
    }

    @Override
    public @Nullable XDebugSessionImpl getSession() {
        return mySession;
    }

    protected void beforeTreeBuild(SessionEvent event) {
    }

    @RequiredUIAccess
    protected @Nullable Component createTopPanel() {
        return null;
    }

    @Override
    public Component getPanel() {
        return myComponent;
    }

    @Override
    public void processSessionEvent(SessionEvent event, XDebugSession session) {
        // mark nodes obsolete asap
        getTree().markNodesObsolete();

        XStackFrame stackFrame = session.getCurrentStackFrame();
        getTree().getUIAccess().give(() -> {
            if (event == SessionEvent.BEFORE_RESUME || event == SessionEvent.SETTINGS_CHANGED) {
                saveCurrentTreeState(stackFrame);
                if (event == SessionEvent.BEFORE_RESUME) {
                    return;
                }
            }

            getTree().markNodesObsolete();
            if (stackFrame != null) {
                cancelClear();
                beforeTreeBuild(event);
                buildTreeAndRestoreState(stackFrame);
            }
            else {
                requestClear();
            }
        });
    }

    @RequiredUIAccess
    protected void addEmptyMessage(UnifiedXValueContainerNode<?> root) {
        XDebugSession session = getSession();
        if (session != null) {
            if (!session.isStopped() && session.isPaused()) {
                root.setInfoMessage(LocalizeValue.localizeTODO("Frame is not available"));
            }
            else {
                XDebugProcess debugProcess = session.getDebugProcess();
                root.setInfoMessage(debugProcess.getCurrentStateMessage());
            }
        }
    }

    @Override
    protected void clear() {
        getTree().getUIAccess().giveIfNeed(() -> {
            UnifiedXValueContainerNode<?> root = createNewRootNode(null);
            addEmptyMessage(root);
            super.clear();
        });
    }
}

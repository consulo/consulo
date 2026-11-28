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
package consulo.execution.debug.impl.internal.memory.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.frame.XValue;
import consulo.execution.debug.impl.internal.memory.ui.XInstancesWindow;
import consulo.execution.debug.impl.internal.ui.tree.action.XDebuggerTreeActionBase;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.memory.HasMemoryView;
import consulo.execution.debug.memory.TypeInfo;
import consulo.execution.debug.memory.XMemoryViewHandler;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import org.jspecify.annotations.Nullable;

/**
 * Shows the instances of the type of the selected variable.
 *
 * @author VISTALL
 * @since 2026-11-17
 */
@ActionImpl(
    id = "MemoryView.ShowInstancesOfValue",
    parents = @ActionParentRef(@ActionRef(id = "XDebugger.ValueGroup"))
)
public class ShowInstancesAction extends AnAction implements AnActionWithSyncUpdate {
    public ShowInstancesAction() {
        super(XDebuggerLocalize.actionMemoryShowInstancesText());
    }

    @Override
    public void update(AnActionEvent e) {
        XDebugSession session = e.getData(XDebugSession.DATA_KEY);
        e.getPresentation().setEnabledAndVisible(session != null && session.isSuspended() && getTypeInfo(e, session) != null);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        XDebugSession session = e.getData(XDebugSession.DATA_KEY);
        if (session == null || !(session.getDebugProcess() instanceof HasMemoryView hasMemoryView)) {
            return;
        }
        TypeInfo type = getTypeInfo(e, session);
        if (type != null) {
            new XInstancesWindow(session, hasMemoryView.getMemoryViewHandler(), type).show();
        }
    }

    private static @Nullable TypeInfo getTypeInfo(AnActionEvent e, XDebugSession session) {
        if (!(session.getDebugProcess() instanceof HasMemoryView hasMemoryView)) {
            return null;
        }
        XValue value = XDebuggerTreeActionBase.getSelectedValue(e.getDataContext());
        if (value == null) {
            return null;
        }
        XMemoryViewHandler handler = hasMemoryView.getMemoryViewHandler();
        TypeInfo type = handler.getTypeInfo(value);
        return type != null && type.canGetInstanceInfo() ? type : null;
    }
}

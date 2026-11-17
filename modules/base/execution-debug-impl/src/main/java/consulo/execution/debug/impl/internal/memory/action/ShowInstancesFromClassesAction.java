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
import consulo.execution.debug.impl.internal.memory.ui.ClassesTable;
import consulo.execution.debug.impl.internal.memory.ui.XClassesView;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.memory.TypeInfo;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;

/**
 * Shows the instances of the class selected in the Memory view.
 *
 * @author VISTALL
 * @since 2026-11-17
 */
@ActionImpl(id = "MemoryView.ShowInstances")
public class ShowInstancesFromClassesAction extends AnAction implements AnActionWithSyncUpdate {
    public ShowInstancesFromClassesAction() {
        super(XDebuggerLocalize.actionMemoryShowInstancesText());
    }

    @Override
    public void update(AnActionEvent e) {
        TypeInfo type = e.getData(ClassesTable.SELECTED_CLASS_KEY);
        e.getPresentation().setEnabledAndVisible(type != null && type.canGetInstanceInfo() && e.getData(XClassesView.KEY) != null);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        TypeInfo type = e.getData(ClassesTable.SELECTED_CLASS_KEY);
        XClassesView view = e.getData(XClassesView.KEY);
        if (type != null && view != null) {
            view.showInstances(type);
        }
    }
}

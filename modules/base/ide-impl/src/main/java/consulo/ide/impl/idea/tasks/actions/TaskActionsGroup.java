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
package consulo.ide.impl.idea.tasks.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.task.impl.internal.action.ConfigureServersAction;
import consulo.task.localize.TaskLocalize;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author UNV
 * @since 2025-08-01
 */
@ActionImpl(
    id = "task.actions",
    children = {
        @ActionRef(type = SwitchTaskAction.class),
        @ActionRef(type = GotoTaskAction.class),
        @ActionRef(type = CloseTaskAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = CreateChangelistAction.class),
        @ActionRef(type = ShowTaskDescription.class),
        @ActionRef(type = OpenTaskInBrowserAction.class),
        @ActionRef(type = AnalyzeTaskStacktraceAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = ConfigureServersAction.class)
    }
)
public class TaskActionsGroup extends DefaultActionGroup implements DumbAware {
    public TaskActionsGroup() {
        super(TaskLocalize.groupTaskActionsText(), false);
    }
}

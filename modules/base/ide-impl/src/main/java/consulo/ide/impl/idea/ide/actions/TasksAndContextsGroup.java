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
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.tasks.actions.TaskActionsGroup;
import consulo.ide.impl.idea.tasks.actions.context.WorkingContextGroup;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author UNV
 * @since 2025-08-01
 */
@ActionImpl(
    id = "tasks.and.contexts",
    children = {
        @ActionRef(type = TaskActionsGroup.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = WorkingContextGroup.class)
    }
)
public class TasksAndContextsGroup extends DefaultActionGroup implements DumbAware {
    public TasksAndContextsGroup() {
        super(LocalizeValue.localizeTODO("_Tasks & Contexts"), true);
    }
}

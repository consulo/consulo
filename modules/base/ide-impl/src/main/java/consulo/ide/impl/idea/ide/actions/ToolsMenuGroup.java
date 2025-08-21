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
import consulo.ide.impl.idea.internal.psiView.PsiViewerAction;
import consulo.ide.impl.idea.internal.psiView.PsiViewerForContextAction;
import consulo.ide.impl.idea.tools.ExternalToolsGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.task.internal.TaskActions;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author UNV
 * @since 2025-08-01
 */
@ActionImpl(
    id = "ToolsMenu",
    children = {
        @ActionRef(id = TaskActions.TASKS_AND_CONTEXTS),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = ToolsBasicGroup.class),
        @ActionRef(type = PsiViewerAction.class),
        @ActionRef(type = PsiViewerForContextAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = CreateLauncherScriptAction.class),
        @ActionRef(type = CreateDesktopEntryAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = ExternalToolsGroup.class)
    }
)
public class ToolsMenuGroup extends DefaultActionGroup implements DumbAware {
    public ToolsMenuGroup() {
        super(ActionLocalize.groupToolsmenuText(), true);
    }
}

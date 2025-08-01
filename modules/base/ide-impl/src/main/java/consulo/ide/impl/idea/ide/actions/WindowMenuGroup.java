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
import consulo.ide.impl.idea.openapi.wm.impl.ProjectWindowActionGroup;
import consulo.ide.impl.idea.openapi.wm.impl.status.BackgroundTasksGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.ui.impl.internal.wm.action.ActiveToolWindowGroup;
import consulo.project.ui.impl.internal.wm.action.RestoreDefaultLayoutAction;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author UNV
 * @since 2025-07-31
 */
@ActionImpl(
    id = "WindowMenu",
    children = {
        @ActionRef(type = StoreDefaultLayoutAction.class),
        @ActionRef(type = RestoreDefaultLayoutAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = ActiveToolWindowGroup.class),
        @ActionRef(type = EditorTabsGroup.class),
        @ActionRef(type = BackgroundTasksGroup.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = ProjectWindowActionGroup.class)
    }
)
public class WindowMenuGroup extends DefaultActionGroup implements DumbAware {
    public WindowMenuGroup() {
        super(ActionLocalize.groupWindowmenuText(), true);
    }
}

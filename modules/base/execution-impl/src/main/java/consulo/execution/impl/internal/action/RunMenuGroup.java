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
package consulo.execution.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.application.dumb.DumbAware;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;

/**
 * @author UNV
 * @since 2025-09-06
 */
@ActionImpl(
    id = IdeActions.GROUP_RUN,
    children = {
        @ActionRef(type = RunnerActionsGroup.class),
        @ActionRef(type = ChooseRunConfigurationPopupAction.class),
        @ActionRef(id = "ChooseDebugConfiguration"),
        @ActionRef(type = EditRunConfigurationsAction.class),
        @ActionRef(type = StopAction.class),
        @ActionRef(type = StopBackgroundProcessesAction.class),
        @ActionRef(type = ShowRunningListAction.class)
    },
    parents = @ActionParentRef(
        value = @ActionRef(id = IdeActions.GROUP_MAIN_MENU),
        anchor = ActionRefAnchor.AFTER,
        relatedToAction = @ActionRef(id = IdeActions.GROUP_REFACTOR)
    )
)
public class RunMenuGroup extends DefaultActionGroup implements DumbAware {
    public RunMenuGroup() {
        super(ActionLocalize.groupRunmenuText(), true);
    }
}

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
package consulo.execution.debug.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;

/**
 * @author UNV
 * @since 2025-09-06
 */
@ActionImpl(
    id = "DebugMainMenu",
    children = {
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = StepOverAction.class),
        @ActionRef(type = ForceStepOverAction.class),
        @ActionRef(type = StepIntoAction.class),
        @ActionRef(type = ForceStepIntoAction.class),
        @ActionRef(type = SmartStepIntoAction.class),
        @ActionRef(type = StepOutAction.class),
        @ActionRef(type = RunToCursorAction.class),
        @ActionRef(type = ForceRunToCursorAction.class),
        @ActionRef(type = ResetFrameAction.class),
        @ActionRef(type = PauseAction.class),
        @ActionRef(type = ResumeAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = EvaluateAction.class),
        @ActionRef(type = QuickEvaluateAction.class),
        @ActionRef(type = ShowExecutionPointAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = ToggleLineBreakpointAction.class),
        @ActionRef(type = ToggleTemporaryLineBreakpointAction.class),
        @ActionRef(type = ViewBreakpointsAction.class),
        @ActionRef(type = AnSeparator.class)
    },
    parents = @ActionParentRef(value = @ActionRef(id = IdeActions.GROUP_RUN))
)
public class DebugMainMenuGroup extends DefaultActionGroup implements DumbAware {
    public DebugMainMenuGroup() {
        super(ActionLocalize.groupDebugmainmenuText(), false);
    }
}

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
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.execution.debug.XDebuggerActions;
import consulo.execution.debug.impl.internal.stream.action.TraceStreamAction;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.MoreActionGroup;

/**
 * @author VISTALL
 */
@ActionImpl(
    id = XDebuggerActions.TOOL_WINDOW_TOP_TOOLBAR_EXTRA_GROUP,
    children = {
        @ActionRef(type = ForceStepOverAction.class),
        @ActionRef(type = ForceStepIntoAction.class),
        @ActionRef(type = SmartStepIntoAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = RunToCursorAction.class),
        @ActionRef(type = ForceRunToCursorAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = ShowExecutionPointAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = EvaluateAction.class),
        @ActionRef(type = ResetFrameAction.class),
        @ActionRef(type = TraceStreamAction.class)
    }
)
public class DebugToolWindowTopToolbarExtraGroup extends MoreActionGroup implements DumbAware {
    public DebugToolWindowTopToolbarExtraGroup() {
        super(false);
        setPopup(true);
    }
}

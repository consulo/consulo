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
import consulo.execution.debug.impl.internal.frame.action.*;
import consulo.execution.debug.impl.internal.ui.tree.action.*;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author UNV
 * @since 2025-10-08
 */
@ActionImpl(
    id = XDebuggerActions.KEYMAP_GROUP,
    children = {
        @ActionRef(type = XSetValueAction.class),
        @ActionRef(type = XCopyValueAction.class),
        @ActionRef(type = XCompareWithClipboardAction.class),
        @ActionRef(type = XCopyNameAction.class),
        @ActionRef(type = XInspectAction.class),
        @ActionRef(type = XJumpToSourceAction.class),
        @ActionRef(type = XJumpToTypeSourceAction.class),

        @ActionRef(type = XAddToWatchesTreeAction.class),
        @ActionRef(type = EvaluateInConsoleFromTreeAction.class),

        @ActionRef(type = XNewWatchAction.class),
        @ActionRef(type = XEditWatchAction.class),
        @ActionRef(type = XCopyWatchAction.class),
        @ActionRef(type = XRemoveWatchAction.class),
        @ActionRef(type = XRemoveAllWatchesAction.class),
        @ActionRef(type = MuteBreakpointAction.class),
        @ActionRef(type = SortValuesToggleAction.class),
        @ActionRef(type = MarkObjectAction.class),
        @ActionRef(type = FocusOnBreakpointAction.class),
        @ActionRef(type = ShowReferringObjectsAction.class)
    }
)
public class XDebuggerActionGroup extends DefaultActionGroup implements DumbAware {
    public XDebuggerActionGroup() {
        super(LocalizeValue.absent(), false);
    }
}

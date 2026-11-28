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
package consulo.execution.debug.impl.internal.action.thread;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.execution.debug.XDebuggerActions;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * The popup of the Threads view - plugins add the actions of their threads by {@link XDebuggerActions#THREADS_VIEW_POPUP}.
 *
 * @author VISTALL
 * @since 2026-11-17
 */
@ActionImpl(
    id = XDebuggerActions.THREADS_VIEW_POPUP,
    children = {
        @ActionRef(type = ResumeThreadAction.class),
        @ActionRef(type = FreezeThreadAction.class),
        @ActionRef(type = InterruptThreadAction.class)
    }
)
public class ThreadsViewPopupGroup extends DefaultActionGroup {
}

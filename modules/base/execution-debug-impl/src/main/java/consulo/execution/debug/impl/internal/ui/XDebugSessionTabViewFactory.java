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
package consulo.execution.debug.impl.internal.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.execution.debug.impl.internal.XDebugSessionImpl;
import consulo.execution.debug.impl.internal.frame.XDebugView;
import consulo.execution.debug.impl.internal.frame.XWatchesView;
import consulo.execution.debug.ui.XDebugTabLayouter;
import consulo.execution.ui.layout.RunnerLayoutUi;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;

/**
 * Builds the views of a debug session tab - the frames and the variables - for the frontend the tab is shown on.
 * The swing views need an awt hierarchy to be drawn, a unified frontend gets views of its own.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface XDebugSessionTabViewFactory {
    /**
     * A view of the tab, and the content it is shown in.
     */
    record ViewContent(XDebugView view, Content content) {
    }

    static XDebugSessionTabViewFactory getInstance() {
        return Application.get().getInstance(XDebugSessionTabViewFactory.class);
    }

    /**
     * The stack frames of the current execution stack - selecting one changes the current frame of the session.
     */
    @RequiredUIAccess
    ViewContent createFramesView(XDebugSessionImpl session, RunnerLayoutUi ui);

    /**
     * The variables of the current frame with the watches before them - the view is the {@link XWatchesView} of the tab.
     */
    @RequiredUIAccess
    ViewContent createVariablesView(XDebugSessionImpl session, RunnerLayoutUi ui);

    /**
     * The contents a debug process adds to the tab by itself, see {@link XDebugTabLayouter#registerAdditionalContent(RunnerLayoutUi)}.
     */
    @RequiredUIAccess
    void registerAdditionalContent(XDebugTabLayouter layouter, RunnerLayoutUi ui);
}

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

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.execution.debug.XDebuggerActions;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.debug.impl.internal.XDebugSessionImpl;
import consulo.execution.debug.impl.internal.frame.XFramesView;
import consulo.execution.debug.impl.internal.frame.XWatchesViewImpl;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.ui.DebuggerContentInfo;
import consulo.execution.debug.ui.XDebugTabLayouter;
import consulo.execution.ui.layout.RunnerLayoutUi;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.content.Content;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2026-09-24
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.AWT)
public class AWTXDebugSessionTabViewFactory implements XDebugSessionTabViewFactory {
    @RequiredUIAccess
    @Override
    public ViewContent createFramesView(XDebugSessionImpl session, RunnerLayoutUi ui) {
        XFramesView framesView = new XFramesView(session.getProject(), session);
        Content framesContent = ui.createContent(
            DebuggerContentInfo.FRAME_CONTENT,
            framesView.getMainPanel(),
            XDebuggerLocalize.debuggerSessionTabFramesTitle().get(),
            ExecutionDebugIconGroup.nodeFrame(),
            null
        );
        framesContent.setCloseable(false);
        return new ViewContent(framesView, framesContent);
    }

    @RequiredUIAccess
    @Override
    public ViewContent createVariablesView(XDebugSessionImpl session, RunnerLayoutUi ui) {
        XWatchesViewImpl variablesView = new XWatchesViewImpl(session, true);

        Content result = ui.createContent(
            DebuggerContentInfo.VARIABLES_CONTENT,
            variablesView.getPanel(),
            XDebuggerLocalize.debuggerSessionTabVariablesTitle().get(),
            ExecutionDebugIconGroup.nodeValue(),
            null
        );
        result.setCloseable(false);

        ActionGroup group = DebuggerSessionTabBase.customizedActionGroup(XDebuggerActions.VARIABLES_TREE_TOOLBAR_GROUP);
        result.setActions(group, ActionPlaces.DEBUGGER_TOOLBAR, variablesView.getTree());
        return new ViewContent(variablesView, result);
    }

    @RequiredUIAccess
    @Override
    public void registerAdditionalContent(XDebugTabLayouter layouter, RunnerLayoutUi ui) {
        layouter.registerAdditionalContent(ui);
    }
}

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
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.debug.impl.internal.XDebugSessionImpl;
import consulo.execution.debug.impl.internal.frame.UnifiedXFramesView;
import consulo.execution.debug.impl.internal.frame.UnifiedXWatchesView;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.ui.DebuggerContentInfo;
import consulo.execution.debug.ui.XDebugTabLayouter;
import consulo.execution.ui.layout.RunnerLayoutUi;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2026-09-24
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedXDebugSessionTabViewFactory implements XDebugSessionTabViewFactory {
    @RequiredUIAccess
    @Override
    public ViewContent createFramesView(XDebugSessionImpl session, RunnerLayoutUi ui) {
        UnifiedXFramesView framesView = new UnifiedXFramesView(session.getProject(), session);
        Content content = ui.createUIContent(
            DebuggerContentInfo.FRAME_CONTENT,
            framesView.getMainPanel(),
            XDebuggerLocalize.debuggerSessionTabFramesTitle().get(),
            ExecutionDebugIconGroup.nodeFrame(),
            null
        );
        content.setCloseable(false);
        return new ViewContent(framesView, content);
    }

    @RequiredUIAccess
    @Override
    public ViewContent createVariablesView(XDebugSessionImpl session, RunnerLayoutUi ui) {
        UnifiedXWatchesView variablesView = new UnifiedXWatchesView(session, true);
        Content content = ui.createUIContent(
            DebuggerContentInfo.VARIABLES_CONTENT,
            variablesView.getPanel(),
            XDebuggerLocalize.debuggerSessionTabVariablesTitle().get(),
            ExecutionDebugIconGroup.nodeValue(),
            null
        );
        content.setCloseable(false);
        return new ViewContent(variablesView, content);
    }

    /**
     * The contents a debug process registers by itself are swing views - the threads of the java debugger, for one -
     * and they are built before they are handed to the layout, so there is no content to fall back to. The threads
     * are switched in the frames view here.
     */
    @RequiredUIAccess
    @Override
    public void registerAdditionalContent(XDebugTabLayouter layouter, RunnerLayoutUi ui) {
    }
}

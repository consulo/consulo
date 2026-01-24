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
package consulo.execution.debug.impl.internal.stream;

import consulo.application.ReadAction;
import consulo.disposer.Disposable;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.event.XDebugSessionListener;
import consulo.execution.debug.impl.internal.stream.action.TraceStreamRunner;
import consulo.execution.debug.stream.ChainStatus;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public final class TraceDebuggerStateListener {
    private volatile ChainStatus lastChainStatus;

    public TraceDebuggerStateListener(@Nonnull XDebugSession session, @Nonnull Disposable disposable) {
        session.addSessionListener(new XDebugSessionListener() {
            @Override
            public void sessionPaused() {
                collect(session);
            }

            @Override
            public void beforeSessionResume() {
                collect(session);
            }

            @Override
            public void sessionResumed() {
                collect(session);
            }

            @Override
            public void sessionStopped() {
                collect(session);
            }

            @Override
            public void stackFrameChanged() {
                collect(session);
            }
        }, disposable);
    }

    private void collect(@Nonnull XDebugSession session) {
        Project project = session.getProject();

        lastChainStatus = ReadAction.compute(() -> TraceStreamRunner.getInstance(project).getChainStatus(session));
    }

    @Nullable
    public ChainStatus getChainStatus() {
        return lastChainStatus;
    }
}


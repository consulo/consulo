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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.execution.debug.XDebugProcess;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.event.XDebuggerManagerListener;
import consulo.execution.debug.stream.ChainStatus;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.concurrent.ConcurrentHashMap;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public final class StreamDebuggerManager implements XDebuggerManagerListener, Disposable {
    private final ConcurrentHashMap<XDebugSession, TraceDebuggerStateListener> sessionStates = new ConcurrentHashMap<>();

    @Inject
    public StreamDebuggerManager(@Nonnull Project project) {
        project.getMessageBus().connect().subscribe(XDebuggerManagerListener.class, this);
    }

    @Override
    public void processStarted(@Nonnull XDebugProcess debugProcess) {
        XDebugSession session = debugProcess.getSession();

        sessionStates.put(session, new TraceDebuggerStateListener(session, this));
    }

    @Override
    public void processStopped(@Nonnull XDebugProcess debugProcess) {
        sessionStates.remove(debugProcess.getSession());
    }

    @Nullable
    public ChainStatus getChainStatus(@Nonnull XDebugSession id) {
        TraceDebuggerStateListener listener = sessionStates.get(id);
        return listener != null ? listener.getChainStatus() : null;
    }

    @Nonnull
    public static StreamDebuggerManager getInstance(@Nonnull Project project) {
        return project.getInstance(StreamDebuggerManager.class);
    }

    @Override
    public void dispose() {
        sessionStates.clear();
    }
}

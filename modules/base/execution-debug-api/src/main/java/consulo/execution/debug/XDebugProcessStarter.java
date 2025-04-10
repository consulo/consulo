/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.execution.debug;

import consulo.execution.debug.ui.XDebugSessionData;
import consulo.process.ExecutionException;
import jakarta.annotation.Nonnull;

/**
 * Factory class for {@link XDebugProcess} implementation. Used by {@link XDebuggerManager} to start a new debugging session
 *
 * @author nik
 */
@FunctionalInterface
public interface XDebugProcessStarter {
    /**
     * Create a new instance of {@link XDebugProcess} implementation. Note that <code>session</code> isn't initialized when this method is
     * called so in order to perform code depending on <code>session</code> parameter override {@link XDebugProcess#sessionInitialized} method
     *
     * @param session session to be passed to {@link XDebugProcess#XDebugProcess} constructor
     * @return new {@link XDebugProcess} instance
     */
    @Nonnull
    public XDebugProcess start(@Nonnull XDebugSession session) throws ExecutionException;

    /**
     * If return true {@link #configure(XDebugSessionData)} will be called and {@link XDebugSession} will be activated
     */
    default boolean canConfigure() {
        return false;
    }

    default void configure(XDebugSessionData data) {
    }
}
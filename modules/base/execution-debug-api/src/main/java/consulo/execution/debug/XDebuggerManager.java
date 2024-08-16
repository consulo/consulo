/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.ui.ExecutionConsole;
import consulo.execution.ui.RunContentDescriptor;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author nik
 */
@ServiceAPI(value = ComponentScope.PROJECT)
public interface XDebuggerManager {

    public static XDebuggerManager getInstance(@Nonnull Project project) {
        return project.getInstance(XDebuggerManager.class);
    }

    @Nonnull
    XBreakpointManager getBreakpointManager();


    @Nonnull
    XDebugSession[] getDebugSessions();

    @Nullable
    XDebugSession getDebugSession(@Nonnull ExecutionConsole executionConsole);

    @Nonnull
    <T extends XDebugProcess> List<? extends T> getDebugProcesses(Class<T> processClass);

    @Nullable
    XDebugSession getCurrentSession();


    /**
     * Start a new debugging session and open 'Debug' tool window
     *
     * @param sessionName                 title of 'Debug' tool window
     * @param icon                        icon of 'Debug' tool window
     * @param showToolWindowOnSuspendOnly if {@code true} 'Debug' tool window won't be shown until debug process is suspended on a breakpoint
     */
    @Nonnull
    XDebugSession startSessionAndShowTab(@Nonnull String sessionName,
                                                         @Nullable Image icon,
                                                         @Nullable RunContentDescriptor contentToReuse,
                                                         boolean showToolWindowOnSuspendOnly,
                                                         @Nonnull XDebugProcessStarter starter) throws ExecutionException;


    /**
     * Start a new debugging session. Use this method only if debugging is started by using standard 'Debug' action i.e. this methods is called
     * from {@link ProgramRunner#execute} method. Otherwise use {@link #startSessionAndShowTab} method
     */
    @Nonnull
    XDebugSession startSession(@Nonnull ExecutionEnvironment environment, @Nonnull XDebugProcessStarter processStarter)
        throws ExecutionException;

    /**
     * Start a new debugging session and open 'Debug' tool window
     *
     * @param sessionName title of 'Debug' tool window
     */
    @Nonnull
    default XDebugSession startSessionAndShowTab(@Nonnull String sessionName,
                                                @Nullable RunContentDescriptor contentToReuse,
                                                @Nonnull XDebugProcessStarter starter) throws ExecutionException {
        return startSessionAndShowTab(sessionName, contentToReuse, false, starter);
    }

    /**
     * Start a new debugging session and open 'Debug' tool window
     *
     * @param sessionName                 title of 'Debug' tool window
     * @param showToolWindowOnSuspendOnly if {@code true} 'Debug' tool window won't be shown until debug process is suspended on a breakpoint
     */
    @Nonnull
    default XDebugSession startSessionAndShowTab(@Nonnull String sessionName,
                                                @Nullable RunContentDescriptor contentToReuse,
                                                boolean showToolWindowOnSuspendOnly,
                                                @Nonnull XDebugProcessStarter starter) throws ExecutionException {
        return startSessionAndShowTab(sessionName, null, contentToReuse, showToolWindowOnSuspendOnly, starter);
    }
}

/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.disposer.Disposable;
import consulo.execution.configuration.RunProfile;
import consulo.execution.debug.breakpoint.XBreakpoint;
import consulo.execution.debug.breakpoint.XLineBreakpoint;
import consulo.execution.debug.event.XDebugSessionListener;
import consulo.execution.debug.frame.XExecutionStack;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debug.frame.XSuspendContext;
import consulo.execution.debug.frame.XValueMarkers;
import consulo.execution.debug.step.XSmartStepIntoHandler;
import consulo.execution.debug.step.XSmartStepIntoVariant;
import consulo.execution.ui.RunContentDescriptor;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.layout.RunnerLayoutUi;
import consulo.project.Project;
import consulo.ui.NotificationType;
import consulo.ui.ex.action.AnAction;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.event.HyperlinkListener;

/**
 * Instances of this class are created by the debugging subsystem when {@link XDebuggerManager#startSession} or
 * {@link XDebuggerManager#startSessionAndShowTab} method is called. It isn't supposed to be implemented by a plugin.
 * <p/>
 * Instance of this class can be obtained from {@link XDebugProcess#getSession()} method and used to control debugging process
 *
 * @author nik
 */
public interface XDebugSession extends AbstractDebuggerSession {
    Key<XDebugSession> DATA_KEY = Key.create("XDebugSessionTab.XDebugSession");

    @Nonnull
    Project getProject();

    @Nonnull
    XDebugProcess getDebugProcess();

    boolean isSuspended();

    @Nullable
    XStackFrame getCurrentStackFrame();

    XSuspendContext getSuspendContext();

    /**
     * Position2D from the current frame
     *
     * @return
     */
    @Nullable
    XSourcePosition getCurrentPosition();

    /**
     * Position2D from the top frame
     *
     * @return
     */
    @Nullable
    XSourcePosition getTopFramePosition();

    void stepOver(boolean ignoreBreakpoints);

    void stepInto();

    void stepOut();

    void forceStepInto();

    void runToPosition(@Nonnull XSourcePosition position, final boolean ignoreBreakpoints);

    void pause();

    void resume();

    void showExecutionPoint();

    void setCurrentStackFrame(@Nonnull XExecutionStack executionStack, @Nonnull XStackFrame frame, boolean isTopFrame);

    default void setCurrentStackFrame(@Nonnull XExecutionStack executionStack, @Nonnull XStackFrame frame) {
        setCurrentStackFrame(executionStack, frame, frame.equals(executionStack.getTopFrame()));
    }

    /**
     * Call this method to setup custom icon and/or error message (it will be shown in tooltip) for breakpoint
     *
     * @param breakpoint   breakpoint
     * @param icon         icon (<code>null</code> if default icon should be used). You can use icons from {@link consulo.execution.debug.icon.ExecutionDebugIconGroup}
     * @param errorMessage an error message if breakpoint isn't successfully registered
     */
    void updateBreakpointPresentation(@Nonnull XLineBreakpoint<?> breakpoint, @Nullable Image icon, @Nullable String errorMessage);

    /**
     * Marks the provide breakpoint as verified in the current session
     */
    void setBreakpointVerified(@Nonnull XLineBreakpoint<?> breakpoint);

    /**
     * Marks the provide breakpoint as invalid in the current session
     */
    void setBreakpointInvalid(@Nonnull XLineBreakpoint<?> breakpoint, @Nullable String errorMessage);

    /**
     * Call this method when a breakpoint is reached if its condition ({@link XBreakpoint#getCondition()}) evaluates to {@code true}.
     * <p/>
     * <strong>The underlying debugging process should be suspended only if the method returns {@code true}. </strong>
     *
     * @param breakpoint             reached breakpoint
     * @param evaluatedLogExpression value of {@link XBreakpoint#getLogExpression()} evaluated in the current context
     * @param suspendContext         context
     * @return <code>true</code> if the debug process should be suspended
     */
    boolean breakpointReached(@Nonnull XBreakpoint<?> breakpoint, @Nullable String evaluatedLogExpression, @Nonnull XSuspendContext suspendContext);

    /**
     * @deprecated use {@link #breakpointReached(consulo.ide.impl.idea.xdebugger.breakpoints.XBreakpoint, String, consulo.ide.impl.idea.xdebugger.frame.XSuspendContext)} instead
     */
    boolean breakpointReached(@Nonnull XBreakpoint<?> breakpoint, @Nonnull XSuspendContext suspendContext);

    /**
     * Call this method when position is reached (e.g. after "Run to cursor" or "Step over" command)
     *
     * @param suspendContext context
     */
    default void positionReached(@Nonnull XSuspendContext suspendContext) {
        positionReached(suspendContext, false);
    }

    /**
     * Call this method when position is reached (e.g. after "Run to cursor" or "Step over" command)
     *
     * @param suspendContext context
     * @param attract        attract to debugger panel, and active breakpoint panel if setting enable
     */
    void positionReached(@Nonnull XSuspendContext suspendContext, boolean attract);

    /**
     * Call this method when session resumed because of some external event, e.g. from the debugger console
     */
    void sessionResumed();

    void stop();

    void setBreakpointMuted(boolean muted);

    boolean areBreakpointsMuted();


    void addSessionListener(@Nonnull XDebugSessionListener listener, @Nonnull Disposable parentDisposable);

    void addSessionListener(@Nonnull XDebugSessionListener listener);

    void removeSessionListener(@Nonnull XDebugSessionListener listener);

    default void reportError(@Nonnull final String message) {
        reportMessage(message, NotificationType.ERROR);
    }

    default void reportMessage(@Nonnull final String message, @Nonnull final NotificationType type) {
        reportMessage(message, type, null);
    }

    void reportMessage(@Nonnull String message, @Nonnull NotificationType type, @Nullable HyperlinkListener listener);

    @Nonnull
    String getSessionName();

    @Nonnull
    RunContentDescriptor getRunContentDescriptor();

    @Nullable
    RunProfile getRunProfile();

    void setPauseActionSupported(boolean isSupported);

    void rebuildViews();

    <V extends XSmartStepIntoVariant> void smartStepInto(XSmartStepIntoHandler<V> handler, V variant);

    void updateExecutionPosition();

    void initBreakpoints();

    ConsoleView getConsoleView();

    RunnerLayoutUi getUI();

    @Nullable
    XValueMarkers<?, ?> getValueMarkers();

    void addRestartActions(AnAction... restartActions);

    void addExtraActions(AnAction... extraActions);

    void addExtraStopActions(AnAction... extraStopActions);

    boolean isReadOnly();

    void setReadOnly(boolean readOnly);
}

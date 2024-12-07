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

import consulo.execution.debug.breakpoint.XBreakpointHandler;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.evaluation.XDebuggerEvaluator;
import consulo.execution.debug.frame.XDropFrameHandler;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debug.frame.XSuspendContext;
import consulo.execution.debug.frame.XValueMarkerProvider;
import consulo.execution.debug.step.XSmartStepIntoHandler;
import consulo.execution.debug.ui.XDebugTabLayouter;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.ui.ExecutionConsole;
import consulo.execution.ui.console.TextConsoleBuilderFactory;
import consulo.process.ProcessHandler;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.util.concurrent.AsyncResult;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.event.HyperlinkListener;

/**
 * Extends this class to provide debugging capabilities for custom language/framework.
 * <p>
 * In order to start debugger by 'Debug' action for a specific run configuration implement {@link ProgramRunner}
 * and call {@link XDebuggerManager#startSession} from {@link ProgramRunner#execute} method
 * <p>
 * Otherwise use method {@link XDebuggerManager#startSessionAndShowTab} to start new debugging session
 *
 * @author nik
 */
public abstract class XDebugProcess {
    private final XDebugSession mySession;
    private ProcessHandler myProcessHandler;

    /**
     * @param session pass <code>session</code> parameter of {@link consulo.xdebugger.XDebugProcessStarter#start} method to this constructor
     */
    protected XDebugProcess(@Nonnull XDebugSession session) {
        mySession = session;
    }

    public final XDebugSession getSession() {
        return mySession;
    }

    /**
     * @return breakpoint handlers which will be used to set/clear breakpoints in the underlying debugging process
     */
    @Nonnull
    public XBreakpointHandler<?>[] getBreakpointHandlers() {
        return XBreakpointHandler.EMPTY_ARRAY;
    }

    /**
     * @return editor provider which will be used to produce editors for "Evaluate" and "Set Value" actions
     */
    @Nonnull
    public abstract XDebuggerEditorsProvider getEditorsProvider();

    /**
     * Called when {@link XDebugSession} is initialized and breakpoints are registered in
     * {@link consulo.ide.impl.idea.xdebugger.breakpoints.XBreakpointHandler}
     */
    public void sessionInitialized() {
    }

    /**
     * Interrupt debugging process and call {@link XDebugSession#positionReached}
     * when next line in current method/function is reached.
     * Do not call this method directly. Use {@link XDebugSession#pause()} instead
     */
    public void startPausing() {
    }

    /**
     * @deprecated Use {@link #startStepOver(XSuspendContext)} instead
     */
    @Deprecated
    public void startStepOver() {
        throw new AbstractMethodError();
    }

    /**
     * Resume execution and call {@link XDebugSession#positionReached}
     * when next line in current method/function is reached.
     * Do not call this method directly. Use {@link XDebugSession#stepOver} instead
     */
    public void startStepOver(@Nullable XSuspendContext context) {
        //noinspection deprecation
        startStepOver();
    }

    /**
     * @deprecated Use {@link #startForceStepInto(XSuspendContext)} instead
     */
    @Deprecated
    public void startForceStepInto() {
        //noinspection deprecation
        startStepInto();
    }

    /**
     * Steps into suppressed call
     * <p>
     * Resume execution and call {@link XDebugSession#positionReached}
     * when next line is reached.
     * Do not call this method directly. Use {@link XDebugSession#forceStepInto} instead
     */
    public void startForceStepInto(@Nullable XSuspendContext context) {
        startStepInto(context);
    }

    /**
     * @deprecated Use {@link #startStepInto(XSuspendContext)} instead
     */
    @Deprecated
    public void startStepInto() {
        throw new AbstractMethodError();
    }

    /**
     * Resume execution and call {@link XDebugSession#positionReached}
     * when next line is reached.
     * Do not call this method directly. Use {@link XDebugSession#stepInto} instead
     */
    public void startStepInto(@Nullable XSuspendContext context) {
        //noinspection deprecation
        startStepInto();
    }

    /**
     * @deprecated Use {@link #startStepOut(XSuspendContext)} instead
     */
    @Deprecated
    public void startStepOut() {
        throw new AbstractMethodError();
    }

    /**
     * Resume execution and call {@link XDebugSession#positionReached}
     * after returning from current method/function.
     * Do not call this method directly. Use {@link XDebugSession#stepOut} instead
     */
    public void startStepOut(@Nullable XSuspendContext context) {
        //noinspection deprecation
        startStepOut();
    }

    /**
     * Implement {@link consulo.ide.impl.idea.xdebugger.stepping.XSmartStepIntoHandler} and return its instance from this method to enable Smart Step Into action
     *
     * @return {@link consulo.ide.impl.idea.xdebugger.stepping.XSmartStepIntoHandler} instance
     */
    @Nullable
    public XSmartStepIntoHandler<?> getSmartStepIntoHandler() {
        return null;
    }

    /**
     * Implement {@link XDropFrameHandler} and return its instance from this method to enable the Drop Frame action.
     */
    @Nullable
    public XDropFrameHandler getDropFrameHandler() {
        return null;
    }

    /**
     * Stop debugging and dispose resources.
     * Do not call this method directly. Use {@link XDebugSession#stop} instead
     */
    public void stop() {
        throw new AbstractMethodError();
    }

    @Nonnull
    public AsyncResult<Void> stopAsync() {
        stop();
        return AsyncResult.done(null);
    }

    /**
     * @deprecated Use {@link #resume(XSuspendContext)} instead
     */
    @Deprecated
    public void resume() {
        throw new AbstractMethodError();
    }

    /**
     * Resume execution.
     * Do not call this method directly. Use {@link XDebugSession#resume} instead
     */
    public void resume(@Nullable XSuspendContext context) {
        //noinspection deprecation
        resume();
    }

    /**
     * @deprecated Use {@link #runToPosition(XSourcePosition, XSuspendContext)} instead
     */
    @Deprecated
    public void runToPosition(@Nonnull XSourcePosition position) {
        throw new AbstractMethodError();
    }

    /**
     * Resume execution and call {@link XDebugSession#positionReached(XSuspendContext)}
     * when <code>position</code> is reached.
     * Do not call this method directly. Use {@link XDebugSession#runToPosition} instead
     *
     * @param position position in source code
     */
    public void runToPosition(@Nonnull XSourcePosition position, @Nullable XSuspendContext context) {
        //noinspection deprecation
        runToPosition(position);
    }

    /**
     * Check is it is possible to perform commands such as resume, step etc. And notify user if necessary
     *
     * @return {@code true} if process can actually perform user requests at this moment
     */
    public boolean checkCanPerformCommands() {
        return true;
    }

    /**
     * Check is it is possible to init breakpoints. Otherwise you should call {@link XDebugSession#initBreakpoints()} at the appropriate time
     */
    public boolean checkCanInitBreakpoints() {
        return true;
    }

    @Nullable
    protected ProcessHandler doGetProcessHandler() {
        return null;
    }

    @Nonnull
    public final ProcessHandler getProcessHandler() {
        if (myProcessHandler == null) {
            myProcessHandler = doGetProcessHandler();
            if (myProcessHandler == null) {
                myProcessHandler = new DefaultDebugProcessHandler();
            }
        }
        return myProcessHandler;
    }

    @Nonnull
    public ExecutionConsole createConsole() {
        return TextConsoleBuilderFactory.getInstance().createBuilder(getSession().getProject()).getConsole();
    }

    /**
     * Override this method to enable 'Mark Object' action
     *
     * @return new instance of {@link XValueMarkerProvider}'s implementation or {@code null} if 'Mark Object' feature isn't supported
     */
    @Nullable
    public XValueMarkerProvider<?, ?> createValueMarkerProvider() {
        return null;
    }

    /**
     * Override this method to provide additional actions in 'Debug' tool window
     */
    public void registerAdditionalActions(@Nonnull DefaultActionGroup leftToolbar, @Nonnull DefaultActionGroup topToolbar, @Nonnull DefaultActionGroup settings) {
    }

    /**
     * @return message to show in Variables View when debugger isn't paused
     */
    public String getCurrentStateMessage() {
        return mySession.isStopped() ? XDebuggerBundle.message("debugger.state.message.disconnected") : XDebuggerBundle
            .message("debugger.state.message.connected");
    }

    @Nullable
    public HyperlinkListener getCurrentStateHyperlinkListener() {
        return null;
    }

    /**
     * Override this method to customize content of tab in 'Debug' tool window
     */
    @Nonnull
    public XDebugTabLayouter createTabLayouter() {
        return new XDebugTabLayouter() {
        };
    }

    /**
     * Add or not SortValuesAction (alphabetically sort)
     *
     * @todo this action should be moved to "Variables" as gear action
     */
    public boolean isValuesCustomSorted() {
        return false;
    }

    @Nullable
    public XDebuggerEvaluator getEvaluator() {
        XStackFrame frame = getSession().getCurrentStackFrame();
        return frame == null ? null : frame.getEvaluator();
    }

    /**
     * Is "isShowLibraryStackFrames" setting respected. If true, ShowLibraryFramesAction will be shown, for example.
     */
    public boolean isLibraryFrameFilterSupported() {
        return false;
    }
}

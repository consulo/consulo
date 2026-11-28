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
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debug.frame.XSuspendContext;
import consulo.execution.debug.frame.XValueMarkerProvider;
import consulo.execution.debug.internal.UnsupportedDebuggerEditorsProvider;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.ui.XDebugTabLayouter;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.ui.ExecutionConsole;
import consulo.execution.ui.console.TextConsoleBuilderFactory;
import consulo.localize.LocalizeValue;
import consulo.process.ProcessHandler;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.util.concurrent.AsyncResult;
import org.jspecify.annotations.Nullable;

import javax.swing.event.HyperlinkListener;

/**
 * Extends this class to provide debugging capabilities for custom language/framework.
 * <p>
 * In order to start debugger by 'Debug' action for a specific run configuration implement {@link ProgramRunner}
 * and call {@link XDebuggerManager#startSession} from {@link ProgramRunner#execute} method
 * <p>
 * Otherwise use method {@link XDebuggerManager#startSessionAndShowTab} to start new debugging session
 * <p>
 * Optional actions are supported by implementing their interfaces - the actions are hidden for processes which do not:
 * {@link consulo.execution.debug.step.HasForceStepInto}, {@link consulo.execution.debug.step.HasSmartStepInto},
 * {@link consulo.execution.debug.step.HasStepIntoMyCode}, {@link consulo.execution.debug.step.HasRunToPosition},
 * {@link consulo.execution.debug.step.HasSetNextStatement}, {@link consulo.execution.debug.frame.HasDropFrame},
 * {@link consulo.execution.debug.frame.HasRunningExecutionStacks}
 *
 * @author nik
 */
public abstract class XDebugProcess {
    private final XDebugSession mySession;
    private ProcessHandler myProcessHandler;

    /**
     * @param session pass <code>session</code> parameter of {@link consulo.xdebugger.XDebugProcessStarter#start} method to this constructor
     */
    protected XDebugProcess(XDebugSession session) {
        mySession = session;
    }

    
    public final XDebugSession getSession() {
        return mySession;
    }

    /**
     * @return breakpoint handlers which will be used to set/clear breakpoints in the underlying debugging process
     */
    public XBreakpointHandler<?>[] getBreakpointHandlers() {
        return XBreakpointHandler.EMPTY_ARRAY;
    }

    /**
     * @return editor provider which will be used to produce editors for "Evaluate" and "Set Value" actions
     */
    public XDebuggerEditorsProvider getEditorsProvider() {
        return UnsupportedDebuggerEditorsProvider.INSTANCE;
    }

    /**
     * Called when {@link XDebugSession} is initialized and breakpoints are registered in
     * {@link XBreakpointHandler}
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
     * Stop debugging and dispose resources.
     * Do not call this method directly. Use {@link XDebugSession#stop} instead
     */
    public void stop() {
        throw new AbstractMethodError();
    }

    
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

    protected @Nullable ProcessHandler doGetProcessHandler() {
        return null;
    }

    
    public final ProcessHandler getProcessHandler() {
        if (myProcessHandler == null) {
            myProcessHandler = doGetProcessHandler();
            if (myProcessHandler == null) {
                myProcessHandler = new DefaultDebugProcessHandler();
            }
        }
        return myProcessHandler;
    }

    
    public ExecutionConsole createConsole() {
        return TextConsoleBuilderFactory.getInstance().createBuilder(getSession().getProject()).getConsole();
    }

    /**
     * Override this method to enable 'Mark Object' action
     *
     * @return new instance of {@link XValueMarkerProvider}'s implementation or {@code null} if 'Mark Object' feature isn't supported
     */
    public @Nullable XValueMarkerProvider<?, ?> createValueMarkerProvider() {
        return null;
    }

    /**
     * Override this method to provide additional actions in 'Debug' tool window
     */
    public void registerAdditionalActions(DefaultActionGroup moreGroup, DefaultActionGroup topToolbar, DefaultActionGroup settings) {
    }

    /**
     * @return message to show in Variables View when debugger isn't paused
     */
    public LocalizeValue getCurrentStateMessage() {
        if (mySession.isStopped()) {
            return XDebuggerLocalize.debuggerStateMessageDisconnected();
        }
        else {
            return XDebuggerLocalize.debuggerStateMessageConnected();
        }
    }

    public @Nullable HyperlinkListener getCurrentStateHyperlinkListener() {
        return null;
    }

    /**
     * Override this method to customize content of tab in 'Debug' tool window
     */
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

    public @Nullable XDebuggerEvaluator getEvaluator() {
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

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
package consulo.sandboxPlugin.ide.debug;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.execution.ExecutionResult;
import consulo.execution.debug.XDebugProcess;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.frame.XSuspendContext;
import consulo.execution.ui.ExecutionConsole;
import consulo.process.ProcessHandler;
import consulo.process.ProcessOutputType;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-10-12
 */
public class SandDebugProcess extends XDebugProcess {
    private static final long STEP_DELAY = 300L;
    private static final long RESUME_DELAY = 2000L;

    private final ExecutionResult myExecutionResult;

    private volatile int myStep;

    public SandDebugProcess(XDebugSession session, ExecutionResult executionResult) {
        super(session);
        myExecutionResult = executionResult;
    }

    @Override
    public XDebuggerEditorsProvider getEditorsProvider() {
        return SandDebuggerEditorsProvider.INSTANCE;
    }

    @Override
    protected ProcessHandler doGetProcessHandler() {
        return myExecutionResult.getProcessHandler();
    }

    @Override
    public ExecutionConsole createConsole() {
        return myExecutionResult.getExecutionConsole();
    }

    @Override
    public void sessionInitialized() {
        suspendLater(STEP_DELAY);
    }

    @Override
    public void startPausing() {
        print("Paused\n");
        suspendLater(0);
    }

    @Override
    public void startStepOver(@Nullable XSuspendContext context) {
        step("Step over\n");
    }

    @Override
    public void startStepInto(@Nullable XSuspendContext context) {
        step("Step into\n");
    }

    @Override
    public void startForceStepInto(@Nullable XSuspendContext context) {
        step("Force step into\n");
    }

    @Override
    public void startStepOut(@Nullable XSuspendContext context) {
        step("Step out\n");
    }

    @Override
    public void runToPosition(XSourcePosition position, @Nullable XSuspendContext context) {
        step("Run to position\n");
    }

    @Override
    public void resume(@Nullable XSuspendContext context) {
        print("Resumed\n");
        myStep++;
        suspendLater(RESUME_DELAY);
    }

    @Override
    public void stop() {
        ProcessHandler processHandler = getProcessHandler();
        if (!processHandler.isProcessTerminated()) {
            processHandler.destroyProcess();
        }
    }

    private void step(String message) {
        print(message);
        myStep++;
        suspendLater(STEP_DELAY);
    }

    private void print(String message) {
        getProcessHandler().notifyTextAvailable(message, ProcessOutputType.SYSTEM);
    }

    private void suspendLater(long delay) {
        int step = myStep;
        AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
            if (getProcessHandler().isProcessTerminated() || getSession().isStopped()) {
                return;
            }
            print("Suspended at step " + step + "\n");
            getSession().positionReached(new SandSuspendContext(step));
        }, delay, TimeUnit.MILLISECONDS);
    }
}

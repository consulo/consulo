/*
 * Copyright 2013-2025 consulo.io
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
package consulo.externalSystem.impl.internal.service.execution;

import consulo.build.ui.process.BuildProcessHandler;
import consulo.externalSystem.model.task.ExternalSystemTask;

import java.io.OutputStream;

/**
 * A {@link BuildProcessHandler} that wraps an {@link ExternalSystemTask}.
 * <p>
 * Attaching this handler to a {@link consulo.build.ui.DefaultBuildDescriptor} enables the stop
 * button in the Build/Sync tool window: pressing it invokes
 * {@link ExternalSystemTask#cancel(consulo.externalSystem.model.task.ExternalSystemTaskNotificationListener...)}
 * on the wrapped task.
 * <p>
 * Call {@link #notifyProcessTerminated(int)} (exit code 0 = success, non-zero = failure) when the
 * task ends so that attached console views receive a proper lifecycle event.
 */
public class ExternalSystemProcessHandler extends BuildProcessHandler {

    private final String myExecutionName;
    private ExternalSystemTask myTask;

    public ExternalSystemProcessHandler(ExternalSystemTask task, String executionName) {
        myTask = task;
        myExecutionName = executionName;
    }

    // ---- BuildProcessHandler --------------------------------------------------------------------

    @Override
    public String getExecutionName() {
        return myExecutionName;
    }

    // ---- ProcessHandler -------------------------------------------------------------------------

    @Override
    protected void destroyProcessImpl() {
        ExternalSystemTask task = myTask;
        if (task != null) {
            // cancel() is a varargs method; calling with no args passes an empty listener array.
            task.cancel();
        }
        notifyProcessDetached();
    }

    @Override
    protected void detachProcessImpl() {
        notifyProcessDetached();
    }

    @Override
    public boolean detachIsDefault() {
        return false;
    }

    @Override
    public OutputStream getProcessInput() {
        return null;
    }

    @Override
    public void notifyProcessTerminated(int exitCode) {
        try {
            super.notifyProcessTerminated(exitCode);
        }
        finally {
            myTask = null;
        }
    }
}

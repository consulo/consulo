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
package consulo.execution.profiler;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A profiled process whose data arrives as a dump file when it ends, such as NYTProf, perf, DTrace or a JFR recording
 * written on exit.
 * <p>
 * The subclass calls {@link #initTargetProcessLifecycleListener(ProcessHandler)} from its constructor. When the process
 * terminates, {@link #readPreparedDump(File, ProgressIndicator)} runs on a background thread with progress, and its result
 * becomes the state of this process.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public abstract class FileBasedProfilerProcess<T extends AttachableTargetProcess> extends ProfilerProcess<T> {
    private static final Logger LOG = Logger.getInstance(FileBasedProfilerProcess.class);

    private final File myDumpFile;
    private final AtomicBoolean myDumpRead = new AtomicBoolean();
    private volatile @Nullable ProcessHandler myProcessHandler;

    protected FileBasedProfilerProcess(Project project, T targetProcess, File dumpFile) {
        super(project, targetProcess);
        myDumpFile = dumpFile;
    }

    public File getDumpFile() {
        return myDumpFile;
    }

    /**
     * Reads the dump once the process terminates, or right away when it already has.
     */
    protected void initTargetProcessLifecycleListener(ProcessHandler processHandler) {
        myProcessHandler = processHandler;
        processHandler.addProcessListener(new ProcessListener() {
            @Override
            public void processTerminated(ProcessEvent event) {
                readDumpInBackground();
            }
        });
        if (processHandler.isProcessTerminated()) {
            readDumpInBackground();
        }
    }

    /**
     * Reads the dump the process left, on a background thread.
     *
     * @return the final state, usually {@link #asProfilerState(ProfilerDumpFileParsingResult, ProfilerDumpWriter)} of a parser's result
     */
    protected abstract ProfilerState readPreparedDump(File file, ProgressIndicator indicator);

    /**
     * Turns a parser's result into a state: {@link DataReady} for a {@link Success}, {@link ProfilingFailed} for a
     * {@link Failure}.
     *
     * @param dumpWriter what saves the data as a snapshot, or null when it cannot be saved
     */
    protected ProfilerState asProfilerState(ProfilerDumpFileParsingResult result, @Nullable ProfilerDumpWriter dumpWriter) {
        return switch (result) {
            case Success success -> new DataReady(success.getData(), dumpWriter);
            case Failure failure -> {
                String message = failure.getMessage();
                yield new ProfilingFailed(
                    message == null ? LocalizeValue.localizeTODO("Failed to read the profiler dump") : LocalizeValue.of(message)
                );
            }
        };
    }

    @Override
    public boolean canStop() {
        ProcessHandler processHandler = myProcessHandler;
        return processHandler != null && !processHandler.isProcessTerminated();
    }

    @Override
    public void stop() {
        ProcessHandler processHandler = myProcessHandler;
        if (processHandler != null && !processHandler.isProcessTerminated()) {
            processHandler.destroyProcess();
        }
    }

    private void readDumpInBackground() {
        if (!myDumpRead.compareAndSet(false, true)) {
            return;
        }

        Project project = getProject();
        if (project.isDisposed()) {
            return;
        }

        new Task.Backgroundable(project, LocalizeValue.localizeTODO("Reading profiler results"), true) {
            @Override
            public void run(ProgressIndicator indicator) {
                setState(readPreparedDump(myDumpFile, indicator));
            }

            @Override
            @RequiredUIAccess
            public void onCancel() {
                setState(new ProfilingFailed(LocalizeValue.localizeTODO("Reading profiler results was cancelled")));
            }

            @Override
            public void onThrowable(Throwable throwable) {
                LOG.error("Failed to read profiler dump " + myDumpFile, throwable);
                String message = throwable.getMessage();
                setState(new ProfilingFailed(
                    message == null ? LocalizeValue.localizeTODO("Failed to read the profiler dump") : LocalizeValue.of(message)
                ));
            }
        }.queue();
    }
}

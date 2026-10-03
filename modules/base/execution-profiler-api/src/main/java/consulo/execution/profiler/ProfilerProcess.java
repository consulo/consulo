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

import consulo.disposer.Disposable;
import consulo.execution.profiler.configuration.ProfilerConfigurationState;
import consulo.logging.Logger;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * One profiled target, shown as a profiling session (see {@link ProfilerToolWindowManager}). It starts {@link Profiling}
 * and moves to a final state when its data is ready or profiling failed.
 * <p>
 * A process which also offers live charts and on-demand captures implements
 * {@link consulo.execution.profiler.live.LiveProfilerProcess}.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public abstract class ProfilerProcess<T extends AttachableTargetProcess> {
    private static final Logger LOG = Logger.getInstance(ProfilerProcess.class);

    private final Project myProject;
    private final T myTargetProcess;
    private final List<Consumer<? super ProfilerState>> myStateListeners = new CopyOnWriteArrayList<>();
    private volatile ProfilerState myState = Profiling.INSTANCE;

    protected ProfilerProcess(Project project, T targetProcess) {
        myProject = project;
        myTargetProcess = targetProcess;
    }

    public Project getProject() {
        return myProject;
    }

    public T getTargetProcess() {
        return myTargetProcess;
    }

    public ProfilerState getState() {
        return myState;
    }

    /**
     * Moves the process to a new state and tells the listeners, on the calling thread.
     */
    protected void setState(ProfilerState state) {
        myState = state;
        for (Consumer<? super ProfilerState> listener : myStateListeners) {
            try {
                listener.accept(state);
            }
            catch (Throwable e) {
                LOG.error("State listener of " + this + " failed", e);
            }
        }
    }

    /**
     * Calls the listener after each state change, on the thread which changed the state, which is usually not the UI
     * thread.
     *
     * @return removes the listener when disposed
     */
    public Disposable addStateListener(Consumer<? super ProfilerState> listener) {
        myStateListeners.add(listener);
        return () -> myStateListeners.remove(listener);
    }

    /**
     * @return when profiling started, in milliseconds since the epoch
     */
    public abstract long getAttachedTimestamp();

    /**
     * @return the help topic of the process tab, or null for none
     */
    public @Nullable String getHelpId() {
        return null;
    }

    /**
     * @return the configuration the process is profiled with
     */
    public abstract ProfilerConfigurationState getProfilerConfiguration();

    /**
     * @return whether {@link #stop()} can end profiling now
     */
    public boolean canStop() {
        return false;
    }

    /**
     * Ends profiling early, if the process supports it. Does nothing otherwise.
     */
    public void stop() {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + myTargetProcess.getFullName() + ")";
    }
}

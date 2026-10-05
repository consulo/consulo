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
package consulo.execution.profiler.impl.internal.session;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.profiler.DataReady;
import consulo.execution.profiler.ProfilerData;
import consulo.execution.profiler.ProfilerProcess;
import consulo.execution.profiler.ProfilerState;
import consulo.execution.profiler.Profiling;
import consulo.execution.profiler.ProfilingFailed;
import consulo.execution.profiler.configuration.ProfilerFeature;
import consulo.execution.profiler.icon.ExecutionProfilerIconGroup;
import consulo.execution.profiler.impl.internal.editor.ProfilerSessionVirtualFile;
import consulo.execution.profiler.impl.internal.toolwindow.ProfilerConfigurationPresentation;
import consulo.execution.profiler.impl.internal.view.ProfilerUIUtil;
import consulo.execution.profiler.live.LiveProfilerProcess;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ProfilerSession implements Disposable {
    private static final Logger LOG = Logger.getInstance(ProfilerSession.class);

    private final Project myProject;
    private final ProfilerProcess<?> myProcess;
    private final String myTitle;
    private final String myTargetName;
    private final LocalizeValue myConfigurationName;
    private final Image myIcon;
    private final @Nullable LiveProfilerProcess myLiveProcess;
    private final Set<ProfilerFeature> myFeatures;
    private final ProfilerLiveHistory myLiveHistory = new ProfilerLiveHistory();
    private final List<ProfilerCapture> myCaptures = new CopyOnWriteArrayList<>();
    private final List<ProfilerSessionListener> myListeners = new CopyOnWriteArrayList<>();

    private @Nullable ProfilerSessionVirtualFile myVirtualFile;
    private @Nullable ProfilerState myAppliedState;
    private LocalizeValue myStatus = LocalizeValue.empty();
    private boolean myStatusError;
    private @Nullable LocalizeValue myFailure;
    private int myCpuRecordingCount;
    private int myHeapDumpCount;
    private int myThreadDumpCount;
    private volatile boolean myRecording;
    private volatile boolean myCommandRunning;
    private volatile boolean myDisposed;

    @RequiredUIAccess
    public ProfilerSession(Project project, ProfilerProcess<?> process) {
        myProject = project;
        myProcess = process;
        myTitle = ProfilerConfigurationPresentation.getTabName(process);
        myTargetName = ProfilerConfigurationPresentation.getTargetName(process);
        myConfigurationName = ProfilerConfigurationPresentation.getName(process.getProfilerConfiguration());
        Image icon = ProfilerConfigurationPresentation.getIcon(process.getProfilerConfiguration());
        myIcon = icon == null ? ExecutionProfilerIconGroup.profile() : icon;
        myLiveProcess = process instanceof LiveProfilerProcess live ? live : null;
        myFeatures = getFeatures(myLiveProcess);

        LiveProfilerProcess liveProcess = myLiveProcess;
        if (liveProcess != null && myFeatures.contains(ProfilerFeature.LIVE_MONITORING)) {
            startMonitoring(liveProcess);
        }

        Disposer.register(this, process.addStateListener(state -> myProject.getUIAccess().give(() -> applyState(myProcess.getState()))));
        applyState(process.getState());
    }

    public Project getProject() {
        return myProject;
    }

    public ProfilerProcess<?> getProcess() {
        return myProcess;
    }

    public String getTitle() {
        return myTitle;
    }

    public String getTargetName() {
        return myTargetName;
    }

    public LocalizeValue getConfigurationName() {
        return myConfigurationName;
    }

    public Image getIcon() {
        return myIcon;
    }

    public Set<ProfilerFeature> getFeatures() {
        return myFeatures;
    }

    public boolean isLive() {
        return myLiveProcess != null;
    }

    public boolean hasLiveMonitoring() {
        return myLiveProcess != null && myFeatures.contains(ProfilerFeature.LIVE_MONITORING);
    }

    public ProfilerLiveHistory getLiveHistory() {
        return myLiveHistory;
    }

    public boolean isDisposed() {
        return myDisposed;
    }

    public boolean isActive() {
        return myProcess.getState().isActive();
    }

    public boolean canStop() {
        return isActive() && myProcess.canStop();
    }

    public List<ProfilerCapture> getCaptures() {
        return List.copyOf(myCaptures);
    }

    @RequiredUIAccess
    public LocalizeValue getStatus() {
        return myStatus;
    }

    @RequiredUIAccess
    public boolean isStatusError() {
        return myStatusError;
    }

    @RequiredUIAccess
    public @Nullable LocalizeValue getFailure() {
        return myFailure;
    }

    public boolean isRecording() {
        return myRecording;
    }

    public boolean isCommandRunning() {
        return myCommandRunning;
    }

    public LocalizeValue getStateText() {
        ProfilerState state = myProcess.getState();
        return switch (state) {
            case Profiling profiling -> myRecording
                ? LocalizeValue.localizeTODO("Recording CPU")
                : LocalizeValue.localizeTODO("Profiling");
            case DataReady dataReady -> LocalizeValue.localizeTODO("Finished");
            case ProfilingFailed failed -> LocalizeValue.localizeTODO("Failed");
        };
    }

    @RequiredUIAccess
    public ProfilerSessionVirtualFile getVirtualFile() {
        ProfilerSessionVirtualFile file = myVirtualFile;
        if (file == null) {
            file = new ProfilerSessionVirtualFile(this);
            myVirtualFile = file;
        }
        return file;
    }

    @RequiredUIAccess
    public @Nullable ProfilerSessionVirtualFile findVirtualFile() {
        return myVirtualFile;
    }

    public Disposable addListener(ProfilerSessionListener listener) {
        myListeners.add(listener);
        return () -> myListeners.remove(listener);
    }

    @RequiredUIAccess
    public void removeCapture(ProfilerCapture capture) {
        if (!myCaptures.remove(capture)) {
            return;
        }

        for (ProfilerSessionListener listener : myListeners) {
            try {
                listener.captureRemoved(this, capture);
            }
            catch (Throwable e) {
                LOG.error("Profiler session listener failed", e);
            }
        }
    }

    @RequiredUIAccess
    public void startCpuRecording() {
        LiveProfilerProcess liveProcess = myLiveProcess;
        if (liveProcess == null) {
            return;
        }

        runCommand(
            LocalizeValue.localizeTODO("Starting CPU recording..."),
            "Can't start CPU recording",
            liveProcess::startCpuRecording,
            ignored -> {
                myRecording = true;
                setStatus(LocalizeValue.localizeTODO("Recording CPU..."), false);
            }
        );
    }

    @RequiredUIAccess
    public void stopCpuRecording() {
        LiveProfilerProcess liveProcess = myLiveProcess;
        if (liveProcess == null) {
            return;
        }

        runCommand(
            LocalizeValue.localizeTODO("Stopping CPU recording..."),
            "Can't stop CPU recording",
            liveProcess::stopCpuRecording,
            data -> {
                myRecording = false;
                myCpuRecordingCount++;
                setStatus(LocalizeValue.localizeTODO("CPU recording finished"), false);
                addCapture(
                    LocalizeValue.localizeTODO("CPU Recording " + myCpuRecordingCount),
                    ExecutionProfilerIconGroup.profilecpu(),
                    data,
                    true
                );
            }
        );
    }

    @RequiredUIAccess
    public void dumpHeap() {
        LiveProfilerProcess liveProcess = myLiveProcess;
        if (liveProcess == null) {
            return;
        }

        runCommand(
            LocalizeValue.localizeTODO("Capturing heap dump..."),
            "Can't capture a heap dump",
            liveProcess::dumpHeap,
            data -> {
                myHeapDumpCount++;
                setStatus(LocalizeValue.localizeTODO("Heap dump captured"), false);
                addCapture(
                    LocalizeValue.localizeTODO("Heap Dump " + myHeapDumpCount),
                    ExecutionProfilerIconGroup.profilememory(),
                    data,
                    true
                );
            }
        );
    }

    @RequiredUIAccess
    public void dumpThreads() {
        LiveProfilerProcess liveProcess = myLiveProcess;
        if (liveProcess == null) {
            return;
        }

        runCommand(
            LocalizeValue.localizeTODO("Capturing thread dump..."),
            "Can't capture a thread dump",
            liveProcess::dumpThreads,
            data -> {
                myThreadDumpCount++;
                setStatus(LocalizeValue.localizeTODO("Thread dump captured"), false);
                addCapture(
                    LocalizeValue.localizeTODO("Thread Dump " + myThreadDumpCount),
                    PlatformIconGroup.actionsDump(),
                    data,
                    true
                );
            }
        );
    }

    @RequiredUIAccess
    public void stop() {
        try {
            myProcess.stop();
        }
        catch (Throwable e) {
            LOG.error("Failed to stop " + myProcess, e);
            setStatus(LocalizeValue.localizeTODO("Can't stop profiling: " + ProfilerUIUtil.describe(e)), true);
        }
        fireStatusChanged();
    }

    @Override
    public void dispose() {
        myDisposed = true;
        myLiveHistory.close();
        myListeners.clear();

        ProfilerSessionVirtualFile file = myVirtualFile;
        if (file != null) {
            file.setValid(false);
        }
        for (ProfilerCapture capture : myCaptures) {
            capture.invalidate();
        }
        myCaptures.clear();
    }

    @Override
    public String toString() {
        return myTitle;
    }

    @RequiredUIAccess
    private void applyState(ProfilerState state) {
        if (myDisposed || myProject.isDisposed() || state == myAppliedState) {
            return;
        }
        boolean initial = myAppliedState == null;
        myAppliedState = state;
        if (!state.isActive()) {
            myRecording = false;
        }

        switch (state) {
            case Profiling profiling -> setStatus(LocalizeValue.localizeTODO("Profiling..."), false);
            case DataReady dataReady -> {
                setStatus(initial ? LocalizeValue.empty() : LocalizeValue.localizeTODO("Profiling finished"), false);
                addCapture(LocalizeValue.localizeTODO("Result"), ExecutionProfilerIconGroup.profile(), dataReady.getData(), false);
            }
            case ProfilingFailed failed -> {
                myFailure = failed.getMessage();
                setStatus(failed.getMessage(), true);
            }
        }

        fireStatusChanged();
    }

    @RequiredUIAccess
    private void addCapture(LocalizeValue title, Image icon, ProfilerData data, boolean closable) {
        ProfilerCapture capture = new ProfilerCapture(this, title, icon, data, closable);
        myCaptures.add(capture);
        for (ProfilerSessionListener listener : myListeners) {
            try {
                listener.captureAdded(this, capture);
            }
            catch (Throwable e) {
                LOG.error("Profiler session listener failed", e);
            }
        }
    }

    @RequiredUIAccess
    private <T> void runCommand(
        LocalizeValue progressText,
        String failurePrefix,
        Supplier<? extends CompletableFuture<? extends T>> command,
        @RequiredUIAccess Consumer<? super T> onSuccess
    ) {
        myCommandRunning = true;
        setStatus(progressText, false);
        fireStatusChanged();

        CompletableFuture<? extends T> future;
        try {
            future = command.get();
        }
        catch (Throwable e) {
            future = CompletableFuture.failedFuture(e);
        }

        future.whenComplete((value, error) -> myProject.getUIAccess().give(() -> {
            if (myDisposed || myProject.isDisposed()) {
                return;
            }

            myCommandRunning = false;
            if (error != null) {
                LOG.warn(failurePrefix + " for " + myProcess, error);
                setStatus(LocalizeValue.localizeTODO(failurePrefix + ": " + ProfilerUIUtil.describe(error)), true);
            }
            else {
                onSuccess.accept(value);
            }
            fireStatusChanged();
        }));
    }

    private void setStatus(LocalizeValue text, boolean error) {
        myStatus = text;
        myStatusError = error;
    }

    @RequiredUIAccess
    private void fireStatusChanged() {
        for (ProfilerSessionListener listener : myListeners) {
            try {
                listener.statusChanged(this);
            }
            catch (Throwable e) {
                LOG.error("Profiler session listener failed", e);
            }
        }
    }

    private void startMonitoring(LiveProfilerProcess liveProcess) {
        try {
            Disposable monitoring = liveProcess.startMonitoring(myLiveHistory);
            Disposer.register(this, monitoring);
        }
        catch (Throwable e) {
            LOG.error("Failed to start live monitoring of " + myProcess, e);
        }
    }

    private static Set<ProfilerFeature> getFeatures(@Nullable LiveProfilerProcess liveProcess) {
        if (liveProcess == null) {
            return Set.of();
        }

        try {
            return Set.copyOf(liveProcess.getFeatures());
        }
        catch (Throwable e) {
            LOG.error("Failed to read the features of " + liveProcess, e);
            return Set.of();
        }
    }
}

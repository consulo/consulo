// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.impl.internal;

import consulo.build.ui.event.FinishBuildEvent;
import consulo.build.ui.event.FinishEvent;
import consulo.build.ui.event.StartEvent;
import consulo.build.ui.impl.internal.event.*;
import consulo.build.ui.localize.BuildLocalize;
import consulo.build.ui.progress.BuildProgress;
import consulo.build.ui.progress.BuildProgressDescriptor;
import consulo.localize.LocalizeValue;

public class BuildRootProgressImpl extends BuildProgressImpl {
    public BuildRootProgressImpl() {
        super(null);
    }

    @Override
    public Object getId() {
        return getBuildId();
    }

    @Override
    protected StartEvent createStartEvent(BuildProgressDescriptor descriptor) {
        return new StartBuildEventImpl(descriptor.getBuildDescriptor(), BuildLocalize.buildStatusRunning());
    }

    @Override
    public BuildProgress<BuildProgressDescriptor> finish() {
        return finish(System.currentTimeMillis(), false, BuildLocalize.buildStatusFinished());
    }

    @Override
    public BuildProgress<BuildProgressDescriptor> finish(long timeStamp, boolean isUpToDate, LocalizeValue message) {
        assertStarted();
        FinishEvent event = new FinishBuildEventImpl(getId(), null, timeStamp, message, new SuccessResultImpl(isUpToDate));
        onEvent(getBuildId(), event);
        return this;
    }

    @Override
    public BuildProgress<BuildProgressDescriptor> fail() {
        return fail(System.currentTimeMillis(), BuildLocalize.buildStatusFailed());
    }

    @Override
    public BuildRootProgressImpl fail(long timeStamp, LocalizeValue message) {
        assertStarted();
        FinishBuildEvent event = new FinishBuildEventImpl(getId(), null, timeStamp, message, new FailureResultImpl());
        onEvent(getBuildId(), event);
        return this;
    }

    @Override
    public BuildProgress<BuildProgressDescriptor> cancel() {
        return cancel(System.currentTimeMillis(), BuildLocalize.buildStatusCancelled());
    }

    @Override
    public BuildRootProgressImpl cancel(long timeStamp, LocalizeValue message) {
        assertStarted();
        FinishBuildEventImpl event = new FinishBuildEventImpl(getId(), null, timeStamp, message, new SkippedResultImpl());
        onEvent(getBuildId(), event);
        return this;
    }
}

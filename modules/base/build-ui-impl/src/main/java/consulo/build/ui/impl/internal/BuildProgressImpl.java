// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.impl.internal;

import consulo.build.ui.BuildDescriptor;
import consulo.build.ui.BuildNotificationsGroups;
import consulo.build.ui.FilePosition;
import consulo.build.ui.event.*;
import consulo.build.ui.impl.internal.event.*;
import consulo.build.ui.issue.BuildIssue;
import consulo.build.ui.progress.BuildProgress;
import consulo.build.ui.progress.BuildProgressDescriptor;
import consulo.build.ui.progress.BuildProgressListener;
import consulo.compiler.CompilerManager;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.util.collection.Lists;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class BuildProgressImpl implements BuildProgress<BuildProgressDescriptor> {
    private final Object myId = new Object();

    @Nullable
    private final BuildProgress<BuildProgressDescriptor> myParentProgress;

    private BuildProgressDescriptor myDescriptor;

    private List<BuildProgressListener> myListeners = Lists.newLockFreeCopyOnWriteList();

    public BuildProgressImpl(@Nullable BuildProgress<BuildProgressDescriptor> parentProgress) {
        myParentProgress = parentProgress;
    }

    protected Object getBuildId() {
        return myDescriptor.getBuildDescriptor().getId();
    }

    @Override
    public void addListener(BuildProgressListener listener) {
        myListeners.add(listener);
    }

    protected void onEvent(Object buildId, BuildEvent event) {
        for (BuildProgressListener listener : myListeners) {
            listener.onEvent(buildId, event);
        }
    }

    
    @Override
    public Object getId() {
        return myId;
    }

    
    @Override
    public BuildProgress<BuildProgressDescriptor> start(BuildProgressDescriptor descriptor) {
        myDescriptor = descriptor;
        StartEvent event = createStartEvent(descriptor);
        onEvent(getBuildId(), event);
        return this;
    }

    
    protected StartEvent createStartEvent(BuildProgressDescriptor descriptor) {
        assert myParentProgress != null;
        return new StartEventImpl(getId(), myParentProgress.getId(), System.currentTimeMillis(), descriptor.getTitle());
    }

    
    @Override
    public BuildProgress<BuildProgressDescriptor> startChildProgress(String title) {
        BuildDescriptor buildDescriptor = myDescriptor.getBuildDescriptor();
        BuildProgressImpl progress = new BuildProgressImpl(this);
        progress.myListeners.addAll(myListeners);
        return progress.start(new BuildProgressDescriptor() {

            
            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public
            
            BuildDescriptor getBuildDescriptor() {
                return buildDescriptor;
            }
        });
    }

    
    @Override
    public BuildProgress<BuildProgressDescriptor> progress(String title) {
        return progress(title, -1, -1, "");
    }

    
    @Override
    public BuildProgress<BuildProgressDescriptor> progress(String title, long total, long progress, String unit) {
        Object parentId = myParentProgress != null ? myParentProgress.getId() : null;
        onEvent(getBuildId(),
            new ProgressBuildEventImpl(getId(), parentId, System.currentTimeMillis(), title, total, progress, unit));
        return this;
    }

    
    @Override
    public BuildProgress<BuildProgressDescriptor> output(String text, boolean stdOut) {
        onEvent(getBuildId(), new OutputBuildEventImpl(getId(), text, stdOut));
        return this;
    }

    
    @Override
    public BuildProgress<BuildProgressDescriptor> fileMessage(String title,
                                                              String message,
                                                              MessageEvent.Kind kind,
                                                              FilePosition filePosition) {
        StringBuilder fileLink = new StringBuilder(filePosition.getFile().getPath());
        if (filePosition.getStartLine() > 0) {
            fileLink.append(":").append(filePosition.getStartLine() + 1);
            if (filePosition.getStartColumn() > 0) {
                fileLink.append(":").append(filePosition.getStartColumn() + 1);
            }
        }
        String detailedMessage = fileLink.toString() + '\n' + message;
        FileMessageEventImpl event =
            new FileMessageEventImpl(getId(), kind, CompilerManager.NOTIFICATION_GROUP, title, detailedMessage, filePosition);
        onEvent(getBuildId(), event);
        return this;
    }

    
    @Override
    public BuildProgress<BuildProgressDescriptor> message(String title,
                                                          String message,
                                                          MessageEvent.Kind kind,
                                                          @Nullable Navigatable navigatable) {
        MessageEventImpl event = new MessageEventImpl(getId(), kind, BuildNotificationsGroups.BUILD_ISSUES, title, message) {
            @Override
            public
            @Nullable
            Navigatable getNavigatable(Project project) {
                return navigatable;
            }
        };
        onEvent(getBuildId(), event);
        return this;
    }

    
    @Override
    public BuildProgress<BuildProgressDescriptor> finish() {
        return finish(false);
    }

    
    @Override
    public BuildProgress<BuildProgressDescriptor> finish(boolean isUpToDate) {
        return finish(System.currentTimeMillis(), isUpToDate, myDescriptor.getTitle());
    }

    
    @Override
    public BuildProgress<BuildProgressDescriptor> finish(long timeStamp) {
        return finish(timeStamp, false, myDescriptor.getTitle());
    }

    
    @Override
    public BuildProgress<BuildProgressDescriptor> finish(long timeStamp, boolean isUpToDate, String message) {
        assertStarted();
        assert myParentProgress != null;
        EventResult result = new SuccessResultImpl(isUpToDate);
        FinishEvent event = new FinishEventImpl(getId(), myParentProgress.getId(), timeStamp, message, result);
        onEvent(getBuildId(), event);
        return myParentProgress;
    }

    
    @Override
    public BuildProgress<BuildProgressDescriptor> fail() {
        return fail(System.currentTimeMillis(), myDescriptor.getTitle());
    }

    
    @Override
    public BuildProgress<BuildProgressDescriptor> fail(long timeStamp, String message) {
        assertStarted();
        assert myParentProgress != null;
        FinishEvent event = new FinishEventImpl(getId(), myParentProgress.getId(), timeStamp, message, new FailureResultImpl());
        onEvent(getBuildId(), event);
        return myParentProgress;
    }

    
    @Override
    public BuildProgress<BuildProgressDescriptor> cancel() {
        return cancel(System.currentTimeMillis(), myDescriptor.getTitle());
    }

    
    @Override
    public BuildProgress<BuildProgressDescriptor> cancel(long timeStamp, String message) {
        assertStarted();
        assert myParentProgress != null;
        FinishEventImpl event = new FinishEventImpl(getId(), myParentProgress.getId(), timeStamp, message, new SkippedResultImpl());
        onEvent(getBuildId(), event);
        return myParentProgress;
    }

    @Override
    
    public BuildProgress<BuildProgressDescriptor> buildIssue(BuildIssue issue, MessageEvent.Kind kind) {
        onEvent(getBuildId(), new BuildIssueEventImpl(getId(), issue, kind));
        return this;
    }

    protected void assertStarted() {
        if (myDescriptor == null) {
            throw new IllegalStateException("The start event was not triggered yet.");
        }
    }
}

// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.impl.internal;

import consulo.build.ui.BuildDescriptor;
import consulo.build.ui.BuildNotificationsGroups;
import consulo.build.ui.FilePosition;
import consulo.build.ui.event.EventResult;
import consulo.build.ui.event.FinishEvent;
import consulo.build.ui.event.MessageEvent;
import consulo.build.ui.event.StartEvent;
import consulo.build.ui.impl.internal.event.*;
import consulo.build.ui.issue.BuildIssue;
import consulo.build.ui.progress.BuildProgress;
import consulo.build.ui.progress.BuildProgressDescriptor;
import consulo.build.ui.progress.BuildProgressListener;
import consulo.compiler.CompilerManager;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class BuildProgressImpl implements BuildProgress<BuildProgressDescriptor> {
  private final Object myId = new Object();
  private final BuildProgressListener myListener;
  @Nullable
  private final BuildProgress<BuildProgressDescriptor> myParentProgress;
  private BuildProgressDescriptor myDescriptor;

  public BuildProgressImpl(BuildProgressListener listener, @Nullable BuildProgress<BuildProgressDescriptor> parentProgress) {
    myListener = listener;
    myParentProgress = parentProgress;
  }

  protected Object getBuildId() {
    return myDescriptor.getBuildDescriptor().getId();
  }

  @Nonnull
  @Override
  public Object getId() {
    return myId;
  }

  @Nonnull
  @Override
  public BuildProgress<BuildProgressDescriptor> start(@Nonnull BuildProgressDescriptor descriptor) {
    myDescriptor = descriptor;
    StartEvent event = createStartEvent(descriptor);
    myListener.onEvent(getBuildId(), event);
    return this;
  }

  @Nonnull
  protected StartEvent createStartEvent(BuildProgressDescriptor descriptor) {
    assert myParentProgress != null;
    return new StartEventImpl(getId(), myParentProgress.getId(), System.currentTimeMillis(), descriptor.getTitle());
  }

  @Nonnull
  @Override
  public BuildProgress<BuildProgressDescriptor> startChildProgress(@Nonnull String title) {
    BuildDescriptor buildDescriptor = myDescriptor.getBuildDescriptor();
    return new BuildProgressImpl(myListener, this).start(new BuildProgressDescriptor() {

      @Nonnull
      @Override
      public String getTitle() {
        return title;
      }

      @Override
      public
      @Nonnull
      BuildDescriptor getBuildDescriptor() {
        return buildDescriptor;
      }
    });
  }

  @Nonnull
  @Override
  public BuildProgress<BuildProgressDescriptor> progress(@Nonnull String title) {
    return progress(title, -1, -1, "");
  }

  @Nonnull
  @Override
  public BuildProgress<BuildProgressDescriptor> progress(@Nonnull String title, long total, long progress, String unit) {
    Object parentId = myParentProgress != null ? myParentProgress.getId() : null;
    myListener.onEvent(getBuildId(),
                       new ProgressBuildEventImpl(getId(), parentId, System.currentTimeMillis(), title, total, progress, unit));
    return this;
  }

  @Nonnull
  @Override
  public BuildProgress<BuildProgressDescriptor> output(@Nonnull String text, boolean stdOut) {
    myListener.onEvent(getBuildId(), new OutputBuildEventImpl(getId(), text, stdOut));
    return this;
  }

  @Nonnull
  @Override
  public BuildProgress<BuildProgressDescriptor> fileMessage(@Nonnull String title,
                                                            @Nonnull String message,
                                                            @Nonnull MessageEvent.Kind kind,
                                                            @Nonnull FilePosition filePosition) {
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
    myListener.onEvent(getBuildId(), event);
    return this;
  }

  @Nonnull
  @Override
  public BuildProgress<BuildProgressDescriptor> message(@Nonnull String title,
                                                        @Nonnull String message,
                                                        @Nonnull MessageEvent.Kind kind,
                                                        @Nullable Navigatable navigatable) {
    MessageEventImpl event = new MessageEventImpl(getId(), kind, BuildNotificationsGroups.BUILD_ISSUES, title, message) {
      @Override
      public
      @Nullable
      Navigatable getNavigatable(@Nonnull Project project) {
        return navigatable;
      }
    };
    myListener.onEvent(getBuildId(), event);
    return this;
  }

  @Nonnull
  @Override
  public BuildProgress<BuildProgressDescriptor> finish() {
    return finish(false);
  }

  @Nonnull
  @Override
  public BuildProgress<BuildProgressDescriptor> finish(boolean isUpToDate) {
    return finish(System.currentTimeMillis(), isUpToDate, myDescriptor.getTitle());
  }

  @Nonnull
  @Override
  public BuildProgress<BuildProgressDescriptor> finish(long timeStamp) {
    return finish(timeStamp, false, myDescriptor.getTitle());
  }

  @Nonnull
  @Override
  public BuildProgress<BuildProgressDescriptor> finish(long timeStamp, boolean isUpToDate, @Nonnull String message) {
    assertStarted();
    assert myParentProgress != null;
    EventResult result = new SuccessResultImpl(isUpToDate);
    FinishEvent event = new FinishEventImpl(getId(), myParentProgress.getId(), timeStamp, message, result);
    myListener.onEvent(getBuildId(), event);
    return myParentProgress;
  }

  @Nonnull
  @Override
  public BuildProgress<BuildProgressDescriptor> fail() {
    return fail(System.currentTimeMillis(), myDescriptor.getTitle());
  }

  @Nonnull
  @Override
  public BuildProgress<BuildProgressDescriptor> fail(long timeStamp, @Nonnull String message) {
    assertStarted();
    assert myParentProgress != null;
    FinishEvent event = new FinishEventImpl(getId(), myParentProgress.getId(), timeStamp, message, new FailureResultImpl());
    myListener.onEvent(getBuildId(), event);
    return myParentProgress;
  }

  @Nonnull
  @Override
  public BuildProgress<BuildProgressDescriptor> cancel() {
    return cancel(System.currentTimeMillis(), myDescriptor.getTitle());
  }

  @Nonnull
  @Override
  public BuildProgress<BuildProgressDescriptor> cancel(long timeStamp, @Nonnull String message) {
    assertStarted();
    assert myParentProgress != null;
    FinishEventImpl event = new FinishEventImpl(getId(), myParentProgress.getId(), timeStamp, message, new SkippedResultImpl());
    myListener.onEvent(getBuildId(), event);
    return myParentProgress;
  }

  @Override
  @Nonnull
  public BuildProgress<BuildProgressDescriptor> buildIssue(@Nonnull BuildIssue issue, @Nonnull MessageEvent.Kind kind) {
    myListener.onEvent(getBuildId(), new BuildIssueEventImpl(getId(), issue, kind));
    return this;
  }

  protected void assertStarted() {
    if (myDescriptor == null) {
      throw new IllegalStateException("The start event was not triggered yet.");
    }
  }
}

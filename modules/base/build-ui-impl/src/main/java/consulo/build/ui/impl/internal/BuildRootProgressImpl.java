// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.impl.internal;

import consulo.build.ui.BuildBundle;
import consulo.build.ui.impl.internal.event.*;
import consulo.build.ui.progress.BuildProgress;
import consulo.build.ui.progress.BuildProgressDescriptor;
import consulo.build.ui.progress.BuildProgressListener;
import consulo.build.ui.event.BuildEventsNls;
import consulo.build.ui.event.FinishBuildEvent;
import consulo.build.ui.event.FinishEvent;
import consulo.build.ui.event.StartEvent;

import jakarta.annotation.Nonnull;

public class BuildRootProgressImpl extends BuildProgressImpl {
  private final BuildProgressListener myListener;

  public BuildRootProgressImpl(BuildProgressListener buildProgressListener) {
    super(buildProgressListener, null);
    myListener = buildProgressListener;
  }

  @Nonnull
  @Override
  public Object getId() {
    return getBuildId();
  }

  @Override
  @Nonnull
  protected StartEvent createStartEvent(BuildProgressDescriptor descriptor) {
    return new StartBuildEventImpl(descriptor.getBuildDescriptor(), BuildBundle.message("build.status.running"));
  }

  @Override
  public @Nonnull
  BuildProgress<BuildProgressDescriptor> finish() {
    return finish(System.currentTimeMillis(), false, BuildBundle.message("build.status.finished"));
  }

  @Nonnull
  @Override
  public BuildProgress<BuildProgressDescriptor> finish(long timeStamp, boolean isUpToDate, @Nonnull @BuildEventsNls.Message String message) {
    assertStarted();
    FinishEvent event = new FinishBuildEventImpl(getId(), null, timeStamp, message, new SuccessResultImpl(isUpToDate));
    myListener.onEvent(getBuildId(), event);
    return this;
  }

  @Nonnull
  @Override
  public BuildProgress<BuildProgressDescriptor> fail() {
    return fail(System.currentTimeMillis(), BuildBundle.message("build.status.failed"));
  }

  @Nonnull
  @Override
  public BuildRootProgressImpl fail(long timeStamp, @Nonnull @BuildEventsNls.Message String message) {
    assertStarted();
    FinishBuildEvent event = new FinishBuildEventImpl(getId(), null, timeStamp, message, new FailureResultImpl());
    myListener.onEvent(getBuildId(), event);
    return this;
  }

  @Nonnull
  @Override
  public BuildProgress<BuildProgressDescriptor> cancel() {
    return cancel(System.currentTimeMillis(), BuildBundle.message("build.status.cancelled"));
  }

  @Nonnull
  @Override
  public BuildRootProgressImpl cancel(long timeStamp, @Nonnull String message) {
    assertStarted();
    FinishBuildEventImpl event = new FinishBuildEventImpl(getId(), null, timeStamp, message, new SkippedResultImpl());
    myListener.onEvent(getBuildId(), event);
    return this;
  }
}

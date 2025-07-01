// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.progress;

import consulo.build.ui.FilePosition;
import consulo.build.ui.event.BuildEventsNls;
import consulo.build.ui.event.MessageEvent;
import consulo.build.ui.issue.BuildIssue;
import consulo.navigation.Navigatable;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface BuildProgress<T extends BuildProgressDescriptor> {
    @Nonnull
    Object getId();

    @Nonnull
    BuildProgress<T> start(@Nonnull T descriptor);

    @Nonnull
    BuildProgress<T> progress(@Nonnull String title);

    @Nonnull
    BuildProgress<T> progress(@Nonnull String title, long total, long progress, String unit);

    @Nonnull
    BuildProgress<T> output(@BuildEventsNls.Message @Nonnull String text, boolean stdOut);

    @Nonnull
    BuildProgress<T> message(
        @BuildEventsNls.Title @Nonnull String title,
        @BuildEventsNls.Message @Nonnull String message,
        @Nonnull MessageEvent.Kind kind,
        @Nullable Navigatable navigatable
    );

    @Nonnull
    BuildProgress<T> fileMessage(
        @BuildEventsNls.Title @Nonnull String title,
        @BuildEventsNls.Message @Nonnull String message,
        @Nonnull MessageEvent.Kind kind,
        @Nonnull FilePosition filePosition
    );

    @Nonnull
    BuildProgress<BuildProgressDescriptor> finish();

    @Nonnull
    BuildProgress<BuildProgressDescriptor> finish(long timeStamp);

    @Nonnull
    BuildProgress<BuildProgressDescriptor> finish(boolean isUpToDate);

    @Nonnull
    BuildProgress<BuildProgressDescriptor> finish(long timeStamp, boolean isUpToDate, @BuildEventsNls.Message @Nonnull String message);

    @Nonnull
    BuildProgress<BuildProgressDescriptor> fail();

    @Nonnull
    BuildProgress<BuildProgressDescriptor> fail(long timeStamp, @BuildEventsNls.Message @Nonnull String message);

    @Nonnull
    BuildProgress<BuildProgressDescriptor> cancel();

    @Nonnull
    BuildProgress<BuildProgressDescriptor> cancel(long timeStamp, @BuildEventsNls.Message @Nonnull String message);

    @Nonnull
    BuildProgress<BuildProgressDescriptor> startChildProgress(@BuildEventsNls.Title @Nonnull String title);

    @Nonnull
    BuildProgress<BuildProgressDescriptor> buildIssue(@Nonnull BuildIssue issue, @Nonnull MessageEvent.Kind kind);
}

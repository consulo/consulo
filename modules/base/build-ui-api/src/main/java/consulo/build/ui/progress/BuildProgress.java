// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.progress;

import consulo.build.ui.FilePosition;
import consulo.build.ui.event.MessageEvent;
import consulo.build.ui.issue.BuildIssue;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import org.jspecify.annotations.Nullable;

public interface BuildProgress<T extends BuildProgressDescriptor> {
    void addListener(BuildProgressListener listener);

    Object getId();

    BuildProgress<T> start(T descriptor);

    BuildProgress<T> progress(LocalizeValue title);

    BuildProgress<T> progress(LocalizeValue title, long total, long progress, String unit);

    BuildProgress<T> output(LocalizeValue text, boolean stdOut);

    BuildProgress<T> message(LocalizeValue title, LocalizeValue message, MessageEvent.Kind kind, @Nullable Navigatable navigatable);

    BuildProgress<T> fileMessage(LocalizeValue title, LocalizeValue message, MessageEvent.Kind kind, FilePosition filePosition);

    BuildProgress<BuildProgressDescriptor> finish();

    BuildProgress<BuildProgressDescriptor> finish(long timeStamp);

    BuildProgress<BuildProgressDescriptor> finish(boolean isUpToDate);

    BuildProgress<BuildProgressDescriptor> finish(long timeStamp, boolean isUpToDate, LocalizeValue message);

    BuildProgress<BuildProgressDescriptor> fail();

    BuildProgress<BuildProgressDescriptor> fail(long timeStamp, LocalizeValue message);

    BuildProgress<BuildProgressDescriptor> cancel();

    BuildProgress<BuildProgressDescriptor> cancel(long timeStamp, LocalizeValue message);

    BuildProgress<BuildProgressDescriptor> startChildProgress(LocalizeValue title);

    BuildProgress<BuildProgressDescriptor> buildIssue(BuildIssue issue, MessageEvent.Kind kind);
}

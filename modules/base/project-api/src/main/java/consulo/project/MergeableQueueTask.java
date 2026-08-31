// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.project;

import consulo.application.progress.ProgressIndicator;
import consulo.disposer.Disposable;
import org.jspecify.annotations.Nullable;

public interface MergeableQueueTask<T extends MergeableQueueTask<T>> extends Disposable {
    @Nullable
    T tryMergeWith(T taskFromQueue);

    void perform(ProgressIndicator indicator, Exception trace);
}

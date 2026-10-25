// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.diff.model;

import consulo.disposer.Disposable;

import java.util.function.Consumer;

public interface DiffViewerScrollRequestProducer {
    /**
     * Delivers the scroll requests to {@code listener} until {@code parentDisposable} goes away; a request made while
     * nobody listened is delivered to the next listener.
     */
    void handleScrollRequests(Disposable parentDisposable, Consumer<DiffViewerScrollRequest> listener);
}

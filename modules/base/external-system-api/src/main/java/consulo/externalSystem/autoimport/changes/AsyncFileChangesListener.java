// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalSystem.autoimport.changes;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.externalSystem.autoimport.settings.AsyncSupplier;
import consulo.externalSystem.internal.ExternalSystemInternalHelper;

import java.util.Set;

/**
 * Filters and delegates file and document events into subscribed listeners.
 * Allows using heavy paths filter, that is defined by {@code filesProvider}.
 * The {@code changesListener}'s execution will be skipped if change events didn't happen in watched files.
 */
public final class AsyncFileChangesListener {
    private AsyncFileChangesListener() {
    }

    public static void subscribeOnDocumentsAndVirtualFilesChanges(
        AsyncSupplier<Set<String>> filesProvider,
        FilesChangesListener listener,
        Disposable parentDisposable
    ) {
        subscribeOnVirtualFilesChanges(true, filesProvider, listener, parentDisposable);
        subscribeOnDocumentsChanges(true, filesProvider, listener, parentDisposable);
    }

    public static void subscribeOnVirtualFilesChanges(
        boolean isIgnoreInternalChanges,
        AsyncSupplier<Set<String>> filesProvider,
        FilesChangesListener listener,
        Disposable parentDisposable
    ) {
        Application.get().getInstance(ExternalSystemInternalHelper.class)
            .subscribeOnVirtualFilesChanges(isIgnoreInternalChanges, filesProvider, listener, parentDisposable);
    }

    public static void subscribeOnDocumentsChanges(
        boolean isIgnoreExternalChanges,
        AsyncSupplier<Set<String>> filesProvider,
        FilesChangesListener listener,
        Disposable parentDisposable
    ) {
        Application.get().getInstance(ExternalSystemInternalHelper.class)
            .subscribeOnDocumentsChanges(isIgnoreExternalChanges, filesProvider, listener, parentDisposable);
    }
}

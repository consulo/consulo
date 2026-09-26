// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalSystem.impl.internal.autoimport.changes;

import consulo.codeEditor.EditorFactory;
import consulo.disposer.Disposable;
import consulo.externalSystem.autoimport.ExternalSystemModificationType;
import consulo.externalSystem.impl.internal.autoimport.AutoImportProjectStatus.Stamp;
import consulo.externalSystem.impl.internal.autoimport.changes.vfs.VirtualFileChangesListener;
import consulo.externalSystem.impl.internal.autoimport.settings.AsyncSupplier;
import consulo.externalSystem.impl.internal.util.prefixTree.CanonicalPathPrefixTree;
import consulo.externalSystem.impl.internal.util.prefixTree.set.PrefixTreeSet;
import consulo.externalSystem.impl.internal.util.prefixTree.set.PrefixTreeSets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Filters and delegates file and document events into subscribed listeners.
 * Allows using heavy paths filter, that is defined by {@code filesProvider}.
 * The {@code changesListener}'s execution will be skipped if change events didn't happen in watched files.
 */
public class AsyncFileChangesListener {
    private final AsyncSupplier<Set<String>> myFilesProvider;
    private final FilesChangesListener myChangesListener;

    private Map<String, ModificationData> myUpdatedFiles = new HashMap<>();

    public AsyncFileChangesListener(AsyncSupplier<Set<String>> filesProvider, FilesChangesListener changesListener) {
        myFilesProvider = filesProvider;
        myChangesListener = changesListener;
    }

    public void init() {
        myUpdatedFiles = new HashMap<>();
    }

    public void onFileChange(String path, long modificationStamp, ExternalSystemModificationType modificationType) {
        myUpdatedFiles.put(path, new ModificationData(modificationStamp, modificationType));
    }

    public void apply() {
        Stamp stamp = Stamp.nextStamp();
        Map<String, ModificationData> updatedFilesSnapshot = myUpdatedFiles;
        myFilesProvider.supply(filesToWatch -> {
            PrefixTreeSet<String> index = PrefixTreeSets.toPrefixTreeSet(filesToWatch, CanonicalPathPrefixTree.INSTANCE);
            List<Map.Entry<String, ModificationData>> updatedWatchedFiles = new ArrayList<>();
            for (Map.Entry<String, ModificationData> entry : updatedFilesSnapshot.entrySet()) {
                for (String path : index.getDescendants(entry.getKey())) {
                    updatedWatchedFiles.add(Map.entry(path, entry.getValue()));
                }
            }
            if (!updatedWatchedFiles.isEmpty()) {
                myChangesListener.init();
                for (Map.Entry<String, ModificationData> entry : updatedWatchedFiles) {
                    ModificationData modificationData = entry.getValue();
                    myChangesListener.onFileChange(
                        stamp,
                        entry.getKey(),
                        modificationData.modificationStamp(),
                        modificationData.modificationType()
                    );
                }
                myChangesListener.apply();
            }
        });
    }

    private record ModificationData(long modificationStamp, ExternalSystemModificationType modificationType) {
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
        AsyncFileChangesListener fileListener = new AsyncFileChangesListener(filesProvider, listener);
        AsyncVirtualFilesChangesListener virtualFileListener = new AsyncVirtualFilesChangesListener(isIgnoreInternalChanges, fileListener);
        VirtualFileChangesListener.installAsyncVirtualFileListener(virtualFileListener, parentDisposable);
    }

    public static void subscribeOnDocumentsChanges(
        boolean isIgnoreExternalChanges,
        AsyncSupplier<Set<String>> filesProvider,
        FilesChangesListener listener,
        Disposable parentDisposable
    ) {
        AsyncFileChangesListener fileListener = new AsyncFileChangesListener(filesProvider, listener);
        ExternalDocumentChangeTracker externalChangeTracker = ExternalDocumentChangeTracker.install(parentDisposable);
        AsyncDocumentChangesListener documentListener =
            new AsyncDocumentChangesListener(isIgnoreExternalChanges, fileListener, externalChangeTracker::isExternalChangeInProgress);
        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(documentListener, parentDisposable);
    }
}

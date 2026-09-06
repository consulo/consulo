// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.dependencies;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.container.boot.ContainerPathManager;
import consulo.disposer.Disposable;
import consulo.language.index.impl.internal.dependencies.IndexingDependenciesFingerprint.FingerprintImpl;
import consulo.logging.Logger;
import consulo.virtualFileSystem.impl.internal.FSRecords;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Contains {@link AppIndexingDependenciesToken} that is updated each time an indexing-affecting event occurs such as
 * <ul>
 * <li>IDE fingerprint changed (indexers might have changed)</li>
 * <li>File types changed</li>
 * <li>Manual index rebuild was requested</li>
 * <li>Exception during indexing occurred</li>
 * </ul>
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class AppIndexingDependenciesService implements Disposable {
    private static final Logger LOG = Logger.getInstance(AppIndexingDependenciesService.class);

    public static AppIndexingDependenciesService getInstance() {
        return Application.get().getInstance(AppIndexingDependenciesService.class);
    }

    private static Path defaultStoragePath() {
        return Paths.get(ContainerPathManager.get().getSystemPath(), "caches", "indexingStamp.dat");
    }

    private static void requestVfsRebuildDueToError(Throwable reason) {
        LOG.error(reason);
        FSRecords.invalidateCaches();
    }

    private static AppIndexingDependenciesStorage openOrInitStorage(Path storagePath) {
        try {
            return AppIndexingDependenciesStorage.openOrInit(storagePath);
        }
        catch (IOException e) {
            //FIXME [AK/LK]: don't invalidate VFS if something wrong with indexingStamp -- invalidate indexingStamp itself
            requestVfsRebuildDueToError(e);
            try {
                Files.deleteIfExists(storagePath);
            }
            catch (IOException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw new UncheckedIOException(e);
        }
    }

    private record AppIndexingDependenciesTokenImpl(int appIndexingRequestId) implements AppIndexingDependenciesToken {
        @Override
        public int toInt() {
            return appIndexingRequestId;
        }

        @Override
        public AppIndexingDependenciesToken mergeWith(AppIndexingDependenciesToken other) {
            return new AppIndexingDependenciesTokenImpl(
                Math.max(appIndexingRequestId, ((AppIndexingDependenciesTokenImpl)other).appIndexingRequestId));
        }
    }

    private final AtomicReference<AppIndexingDependenciesTokenImpl> myCurrent =
        new AtomicReference<>(new AppIndexingDependenciesTokenImpl(0));

    private final AtomicReference<FingerprintImpl> myLatestFingerprint =
        new AtomicReference<>(IndexingDependenciesFingerprint.NULL_FINGERPRINT);

    private final AppIndexingDependenciesStorage myStorage;

    @Inject
    public AppIndexingDependenciesService() {
        this(defaultStoragePath());
    }

    public AppIndexingDependenciesService(Path storagePath) {
        myStorage = openOrInitStorage(storagePath);

        try {
            boolean[] shouldMigrateV0toV1 = {false};
            myStorage.checkVersion((expectedVersion, actualVersion) -> {
                if (actualVersion == 0 && expectedVersion == 1) {
                    shouldMigrateV0toV1[0] = true;
                }
                else {
                    requestVfsRebuildAndResetStorage(new IOException("Incompatible version change in AppIndexingDependenciesStorage: "
                        + actualVersion + " to " + expectedVersion));
                }
            });
            if (shouldMigrateV0toV1[0]) {
                migrateV0toV1();
            }
            int appIndexingRequestId = myStorage.readRequestId();
            myCurrent.set(new AppIndexingDependenciesTokenImpl(appIndexingRequestId));
        }
        catch (IOException e) {
            requestVfsRebuildAndResetStorage(e);
            // we don't rethrow exception, because this will put IDE in unusable state.
        }
    }

    private void migrateV0toV1() throws IOException {
        myStorage.writeAppFingerprint(IndexingDependenciesFingerprint.NULL_FINGERPRINT);
        myStorage.completeMigration();
    }

    private void requestVfsRebuildAndResetStorage(IOException reason) {
        try {
            // TODO-ank: we don't need VFS rebuild. It's enough to rebuild indexing stamp attribute storage
            requestVfsRebuildDueToError(reason);
        }
        finally {
            try {
                myStorage.resetStorage();
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            myCurrent.set(new AppIndexingDependenciesTokenImpl(0));
            myLatestFingerprint.set(IndexingDependenciesFingerprint.NULL_FINGERPRINT);
        }
    }

    public AppIndexingDependenciesToken getCurrent() {
        FingerprintImpl fingerprint = IndexingDependenciesFingerprint.getInstance().getFingerprint();
        if (myLatestFingerprint.get().equals(IndexingDependenciesFingerprint.NULL_FINGERPRINT)) {
            try {
                myLatestFingerprint.compareAndSet(IndexingDependenciesFingerprint.NULL_FINGERPRINT, myStorage.readAppFingerprint());
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        FingerprintImpl latestFingerprintValue = myLatestFingerprint.get();
        if (!latestFingerprintValue.equals(fingerprint)) {
            invalidateAllStamps("App fingerprint changed: " + latestFingerprintValue + " to " + fingerprint);
            try {
                myStorage.writeAppFingerprint(fingerprint);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            myLatestFingerprint.compareAndSet(latestFingerprintValue, fingerprint);
        }

        return myCurrent.get();
    }

    public void invalidateAllStamps(String debugReason) {
        LOG.info("Invalidating all indexing flags. Reason: " + debugReason);

        AppIndexingDependenciesTokenImpl next =
            myCurrent.updateAndGet(it -> new AppIndexingDependenciesTokenImpl(it.appIndexingRequestId() + 1));

        // Assumption is that projectStamp >=0 and appStamp >=0. Their sum can be negative and this is fine
        // (think of it as of unsigned int).
        if (next.appIndexingRequestId() < 0) {
            requestVfsRebuildAndResetStorage(new IOException("App indexing stamp overflow"));
        }
        else {
            // don't use `next`: current.get() will return just updated value or more up-to-date value which might has already
            // been persisted by another thread
            try {
                myStorage.writeRequestId(myCurrent.get().appIndexingRequestId());
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public void dispose() {
        try {
            myStorage.close();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public AppIndexingDependenciesToken getCurrentTokenInTest() {
        return myCurrent.get();
    }
}

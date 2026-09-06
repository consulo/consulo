// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.dependencies;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.util.ProjectUtil;
import consulo.virtualFileSystem.impl.internal.FSRecords;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Service that tracks FileIndexingStamp.
 * <p>
 * There are two kinds of tokens: scanning and indexing. Scanning tokens must be explicitly marked as "successfully completed".
 * If there are incomplete or unsuccessful scanning tokens remaining on IDE shutdown, then IDE will do "heavy" scanning on the
 * following start.
 * <p>
 * Indexing tokens are not sensitive to completion. It is expected that during scanning Indexing flag will be cleared for
 * all the files that needs indexing. For files that are sent directly to indexing from VFS refresh we don't need to invalidate
 * indexing flag explicitly, because all these files will have updated modification counter.
 * <p>
 * Notes about "invalidate caches":
 * <p>
 * 1. If VFS is invalidated, we don't need any additional actions. IndexingFlag is stored in the VFS records, invalidating VFS
 * effectively means "reset all the stamps to the default value (unindexed)".
 * <p>
 * 2. If Indexes are invalidated, indexes must call {@link AppIndexingDependenciesService#invalidateAllStamps}, otherwise files
 * that were indexed early will be recognized as "indexed", however real data has been wiped from storages.
 * <p>
 * 3. If int inside {@link AppIndexingDependenciesService} overflows, invalidate VFS storages will help, because all the files
 * fil be marked as "unindexed", and we don't really care if indexing stamp starts counting from 0, or from -42. We only care that after
 * {@link AppIndexingDependenciesService#invalidateAllStamps} invocation "expected" and "actual" stamps are different numbers
 * <p>
 * 4. We don't want "invalidate caches" to drop persistent state. It is OK, if the state is dropped together with VFS invalidation,
 * but persistence should not be dropped in other cases, because IndexingStamp is actually stored in VFS.
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class ProjectIndexingDependenciesService implements Disposable {
    private static final Logger LOG = Logger.getInstance(ProjectIndexingDependenciesService.class);

    public static final FileIndexingStamp NULL_STAMP = NullIndexingStamp.INSTANCE;

    public static ProjectIndexingDependenciesService getInstance(Project project) {
        return project.getInstance(ProjectIndexingDependenciesService.class);
    }

    private static void requestVfsRebuildDueToError(Throwable reason) {
        LOG.error(reason);
        FSRecords.invalidateCaches();
    }

    private static ProjectIndexingDependenciesStorage openOrInitStorage(Path storagePath) {
        try {
            return ProjectIndexingDependenciesStorage.openOrInit(storagePath);
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

    private final Set<Object> myIssuedScanningTokens = new HashSet<>();

    private final AppIndexingDependenciesService myAppIndexingDependenciesService;

    private final ProjectIndexingDependenciesStorage myStorage;

    private volatile boolean myHeavyScanningOnProjectOpen = false;

    @Inject
    public ProjectIndexingDependenciesService(Project project, AppIndexingDependenciesService appIndexingDependenciesService) {
        this(ProjectUtil.getProjectCachePath(project, "indexingStamp").resolve("indexingStamp.dat"), appIndexingDependenciesService);
    }

    public ProjectIndexingDependenciesService(Path storagePath, AppIndexingDependenciesService appIndexingDependenciesService) {
        myAppIndexingDependenciesService = appIndexingDependenciesService;
        myStorage = openOrInitStorage(storagePath);

        try {
            boolean[] shouldMigrateV0toV1 = {false};
            myStorage.checkVersion((expectedVersion, actualVersion) -> {
                if (expectedVersion == 1 && actualVersion == 0) {
                    shouldMigrateV0toV1[0] = true;
                }
                else {
                    requestVfsRebuildAndResetStorage(new IOException("Incompatible version change in ProjectIndexingDependenciesService: "
                        + actualVersion + " to " + expectedVersion));
                }
            });
            if (shouldMigrateV0toV1[0]) {
                migrateV0toV1();
            }

            myHeavyScanningOnProjectOpen = myStorage.readIncompleteScanningMark();
        }
        catch (IOException e) {
            requestVfsRebuildAndResetStorage(e);
            // we don't rethrow exception, because this will put IDE in unusable state.
        }
    }

    private void migrateV0toV1() throws IOException {
        myStorage.writeAppIndexingRequestIdOfLastScanning(
            ProjectIndexingDependenciesStorage.DEFAULT_APP_INDEXING_REQUEST_ID_OF_LAST_COMPLETED_SCANNING);
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
        }
    }

    public IndexingRequestToken getLatestIndexingRequestToken() {
        AppIndexingDependenciesToken appCurrent = myAppIndexingDependenciesService.getCurrent();
        return new IndexingRequestTokenImpl(appCurrent);
    }

    public boolean isScanningAndIndexingCompleted() {
        try {
            return !myStorage.readIncompleteScanningMark();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public int getAppIndexingRequestIdOfLastScanning() {
        try {
            return myStorage.readAppIndexingRequestIdOfLastScanning();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public ScanningRequestToken newScanningToken() {
        AppIndexingDependenciesToken appCurrent = myAppIndexingDependenciesService.getCurrent();
        ScanningRequestToken token = new WriteOnlyScanningRequestTokenImpl(appCurrent, false);
        registerIssuedToken(token);
        return token;
    }

    public ScanningRequestToken newScanningTokenOnProjectOpen(boolean forceCheckingForOutdatedIndexesUsingFileModCount) {
        AppIndexingDependenciesToken appCurrent = myAppIndexingDependenciesService.getCurrent();
        ScanningRequestToken token;
        if (myHeavyScanningOnProjectOpen || myIssuedScanningTokens.contains(RequestFullHeavyScanningToken.INSTANCE)) {
            LOG.info("Heavy scanning on startup because of incomplete scanning from previous IDE session");
            myHeavyScanningOnProjectOpen = false;
            token = new WriteOnlyScanningRequestTokenImpl(appCurrent, forceCheckingForOutdatedIndexesUsingFileModCount);
        }
        else {
            token = new ReadWriteScanningRequestTokenImpl(appCurrent, forceCheckingForOutdatedIndexesUsingFileModCount);
        }
        registerIssuedToken(token);
        completeTokenOrFutureToken(RequestFullHeavyScanningToken.INSTANCE, null, true);
        return token;
    }

    public IncompleteTaskToken newIncompleteTaskToken() {
        IncompleteTaskToken token = new IncompleteTaskToken();
        registerIssuedToken(token);
        return token;
    }

    public IncompleteIndexingToken newIncompleteIndexingToken() {
        IncompleteIndexingToken token = new IncompleteIndexingToken();
        registerIssuedToken(token);
        return token;
    }

    private void registerIssuedToken(Object token) {
        LOG.debug("Register issued token: " + token);
        synchronized (myIssuedScanningTokens) {
            if (myIssuedScanningTokens.isEmpty() && myStorage.isOpen()) {
                LOG.debug("Write incomplete scanning mark=true for token: " + token);
                try {
                    myStorage.writeIncompleteScanningMark(true);
                }
                catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            myIssuedScanningTokens.add(token);
        }
    }

    public void completeToken(IncompleteIndexingToken token) {
        completeTokenOrFutureToken(token, null, token.isSuccessful());
    }

    public void completeToken(IncompleteTaskToken token) {
        completeTokenOrFutureToken(token, null, true);
    }

    public void completeToken(ScanningRequestToken token, boolean isFullScanning) {
        if (token.isSuccessful() && isFullScanning) {
            completeTokenOrFutureToken(RequestFullHeavyScanningToken.INSTANCE, null, true);
        }
        completeTokenOrFutureToken(token, token.getAppIndexingRequestId(), token.isSuccessful());
    }

    private void completeTokenOrFutureToken(Object token,
                                            @Nullable AppIndexingDependenciesToken lastAppIndexingRequestId,
                                            boolean successful) {
        LOG.debug("Complete token: " + token + ", successful: " + successful);
        if (!successful) {
            registerIssuedToken(RequestFullHeavyScanningToken.INSTANCE);
        }
        synchronized (myIssuedScanningTokens) {
            // ignore repeated "complete" calls
            boolean removed = myIssuedScanningTokens.remove(token);
            try {
                if (removed && myIssuedScanningTokens.isEmpty() && myStorage.isOpen()) {
                    LOG.debug("Write incomplete scanning mark=false for token: " + token);
                    myStorage.writeIncompleteScanningMark(false);
                }
                if (lastAppIndexingRequestId != null && myStorage.isOpen()) {
                    // Write each time, not only after the last token has completed, because the last completed token
                    // might be an IncompleteTaskToken. Then lastAppIndexingRequestId will be null.
                    myStorage.writeAppIndexingRequestIdOfLastScanning(lastAppIndexingRequestId.toInt());
                }
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public void requestHeavyScanningOnProjectOpen(String debugReason) {
        LOG.info("Requesting heavy scanning on project open. Reason: " + debugReason);
        registerIssuedToken(RequestFullHeavyScanningToken.INSTANCE);
    }

    @Override
    public void dispose() {
        synchronized (myIssuedScanningTokens) {
            // inside synchronized(issuedScanningTokens) storage.isOpen check should always return the same value
            // to avoid ClosedChannelException from storage.writeIncompleteScanningMark(...)
            try {
                myStorage.close();
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * This token can be thrown away without {@link #completeToken} invocation
     */
    public ScanningRequestToken getReadOnlyTokenForTest() {
        AppIndexingDependenciesToken appCurrent = myAppIndexingDependenciesService.getCurrent();
        return new ReadWriteScanningRequestTokenImpl(appCurrent, true);
    }
}

// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressManager;
import consulo.disposer.Disposer;
import consulo.index.io.IndexExtension;
import consulo.index.io.IndexId;
import consulo.index.io.StorageException;
import consulo.index.io.ID;
import consulo.language.index.impl.internal.stub.StubUpdatingIndex;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileContent;
import consulo.language.psi.stub.SingleEntryFileBasedIndexExtension;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.impl.internal.FSRecords;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.Collection;
import java.util.Map;

public final class StaleIndexesChecker {
    private static final Logger LOG = Logger.getInstance(StaleIndexesChecker.class);
    private static final ThreadLocal<Boolean> IS_IN_STALE_IDS_DELETION = new ThreadLocal<>();

    private StaleIndexesChecker() {
    }

    public static boolean isStaleIdDeletion() {
        return IS_IN_STALE_IDS_DELETION.get() == Boolean.TRUE;
    }

    public static boolean shouldCheckStaleIndexesOnStartup() {
        return !getFreeRecords(true).isEmpty() && ApplicationManager.getApplication().isInternal();
    }

    /**
     * @param knownStaleIds ids to not check
     */
    static IntSet checkIndexForStaleRecords(
        UpdatableIndex<?, ?, FileContent, ?> index,
        IntSet knownStaleIds,
        boolean onStartup
    ) throws StorageException {
        IndexExtension<?, ?, FileContent> extension = index.getExtension();
        IndexId<?, ?> indexId = extension.getName();
        LOG.assertTrue(indexId.equals(StubUpdatingIndex.INDEX_ID), "unexpected index " + indexId);
        LOG.assertTrue(extension instanceof SingleEntryFileBasedIndexExtension, "unexpected extension " + extension);

        Int2ObjectMap<String> staleFiles = new Int2ObjectOpenHashMap<>();
        IntList freeRecords = getFreeRecords(onStartup);
        for (int i = 0; i < freeRecords.size(); i++) {
            int freeRecord = freeRecords.getInt(i);
            if (knownStaleIds.contains(freeRecord)) {
                continue;
            }
            Map<?, ?> dataAsMap = index.getIndexedFileData(freeRecord);
            Object data = ContainerUtil.getFirstItem(dataAsMap.values());
            if (data != null) {
                staleFiles.put(freeRecord, getStaleRecordOrExceptionMessage(freeRecord));
            }
        }

        if (!staleFiles.isEmpty()) {
            if (ApplicationManager.getApplication().isUnitTestMode() && onStartup) {
                // report it as late as possible, give a chance for test to fail by another reason
                Disposer.register(
                    Application.get(),
                    () -> LOG.error(getStaleInputIdsMessage(staleFiles, indexId))
                );
            }
            else {
                LOG.error(getStaleInputIdsMessage(staleFiles, indexId));
            }
        }

        return staleFiles.keySet();
    }

    static String getStaleRecordOrExceptionMessage(int record) {
        try {
            return getRecordPath(record);
        }
        catch (Exception e) {
            return e.getMessage();
        }
    }

    private static String getRecordPath(int record) {
        StringBuilder name = new StringBuilder(FSRecords.getName(record));
        int parent = FSRecords.getParent(record);
        while (parent > 0) {
            name.insert(0, FSRecords.getName(parent) + "/");
            parent = FSRecords.getParent(parent);
        }
        return name.toString();
    }

    static void clearStaleIndexes(IntSet staleIds) {
        IS_IN_STALE_IDS_DELETION.set(Boolean.TRUE);
        boolean unitTest = ApplicationManager.getApplication().isUnitTestMode();
        try {
            ProgressManager.getInstance().executeNonCancelableSection(() -> {
                int maxLogCount = (unitTest || LOG.isDebugEnabled()) ? Integer.MAX_VALUE : 10;
                int loggedCount = 0;
                for (int staleId : staleIds) {
                    if (loggedCount < maxLogCount) {
                        LOG.info("clearing stale id = " + staleId + ", path =  " + getRecordPath(staleId));
                    }
                    else if (loggedCount == maxLogCount) {
                        LOG.info("clearing more items (not logged due to logging limit). "
                            + "Enable debug log for: #" + StaleIndexesChecker.class.getName());
                    }
                    loggedCount++;
                    clearStaleIndexesForId(staleId);
                }
            });
        }
        finally {
            IS_IN_STALE_IDS_DELETION.remove();
        }
    }

    static void clearStaleIndexesForId(int staleInputId) {
        FileBasedIndexImpl fileBasedIndex = (FileBasedIndexImpl) FileBasedIndex.getInstance();
        Collection<ID<?, ?>> indexIds = fileBasedIndex.getRegisteredIndexes().getState().getIndexIDs();
        fileBasedIndex.removeFileDataFromIndices(indexIds, staleInputId, null);
    }

    /**
     * Consulo's {@link FSRecords} does not expose the free (deleted) record lists yet, so no record is checked for staleness.
     */
    private static IntList getFreeRecords(boolean onStartup) {
        return new IntArrayList();
    }

    private static String getStaleInputIdsMessage(Int2ObjectMap<String> staleTrees, IndexId<?, ?> indexId) {
        return "`" + indexId + "` index contains several stale file ids (size = "
            + staleTrees.size()
            + "). Ids & paths: "
            + StringUtil.first(staleTrees.toString(), 300, true)
            + ".";
    }
}

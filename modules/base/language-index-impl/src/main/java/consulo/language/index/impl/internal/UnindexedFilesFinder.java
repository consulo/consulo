// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.language.index.impl.internal;

import consulo.application.AccessRule;
import consulo.application.progress.ProgressManager;
import consulo.index.io.ID;
import consulo.index.io.StorageException;
import consulo.language.impl.internal.psi.stub.FileContentImpl;
import consulo.language.impl.internal.psi.stub.IndexedFileImpl;
import consulo.language.index.impl.internal.dependencies.FileIndexingStamp;
import consulo.language.index.impl.internal.dependencies.ScanningRequestToken;
import consulo.language.index.impl.internal.moduleAware.ModuleAwareIndexMetaRecorder;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.IndexedFile;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.SmartList;
import consulo.util.lang.EmptyRunnable;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.internal.CachedFileType;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

final class UnindexedFilesFinder {
    private static final Logger LOG = Logger.getInstance(UnindexedFilesFinder.class);

    private final Project myProject;
    private final FileBasedIndexImpl myFileBasedIndex;
    private final boolean myDoTraceForFilesToBeIndexed = LOG.isTraceEnabled();
    private final @Nullable BiPredicate<? super IndexedFile, ? super FileIndexingStamp> myForceReindexingTrigger;
    private final ScanningRequestToken myIndexingRequest;

    private static final class UnindexedFileStatusBuilder {
        boolean shouldIndex = false;
        boolean indexesWereProvidedByInfrastructureExtension = false;
        long timeTotalEvaluation = 0;
        long timeProcessingUpToDateFiles = 0;
        long timeUpdatingContentLessIndexes = 0;
        long timeIndexingWithoutContentViaInfrastructureExtension = 0;
        private List<SingleIndexValueApplier> appliers = Collections.emptyList();
        private List<SingleIndexValueRemover> removers = Collections.emptyList();
        final FileIndexingResult.ApplicationMode applicationMode;
        boolean mayMarkFileIndexed = true;
        @Nullable List<Pair<FileIndexingStateWithExplanation, ID<?, ?>>> unindexedStates;

        UnindexedFileStatusBuilder(FileIndexingResult.ApplicationMode applicationMode) {
            this.applicationMode = applicationMode;
        }

        boolean addOrRunRemover(@Nullable SingleIndexValueRemover remover) {
            if (remover == null) {
                return true;
            }

            if (removers.isEmpty()) {
                removers = new SmartList<>();
            }
            return removers.add(remover);
        }

        boolean addOrRunApplier(@Nullable SingleIndexValueApplier applier) {
            if (applier == null) {
                return true;
            }

            if (appliers.isEmpty()) {
                appliers = new SmartList<>();
            }
            return appliers.add(applier);
        }

        void addUnindexedState(FileIndexingStateWithExplanation state, ID<?, ?> id) {
            if (unindexedStates == null) {
                unindexedStates = new ArrayList<>();
            }
            unindexedStates.add(Pair.create(state, id));
        }

        UnindexedFileStatus build() {
            return new UnindexedFileStatus(
                shouldIndex,
                indexesWereProvidedByInfrastructureExtension,
                timeProcessingUpToDateFiles,
                timeUpdatingContentLessIndexes,
                timeIndexingWithoutContentViaInfrastructureExtension,
                timeTotalEvaluation
            );
        }

        void explain(IndexedFile indexedFile) {
            if (shouldIndex) {
                LOG.trace(getIndexingReasonLogString(indexedFile));
            }
            else if (hasAppliersOrRemovers()) {
                LOG.trace(getAppliersAndRemoversLogString(indexedFile));
            }
        }

        boolean hasAppliersOrRemovers() {
            return !appliers.isEmpty() || !removers.isEmpty();
        }

        private String getAppliersAndRemoversLogString(IndexedFile indexedFile) {
            return "Scanner has updated file " + getLogString(indexedFile) +
                " with appliers: " + appliers +
                " and removers: " + removers + "; ";
        }

        private String getIndexingReasonLogString(IndexedFile indexedFile) {
            StringBuilder sb = new StringBuilder("Scheduling indexing of ");
            sb.append(getLogString(indexedFile));
            sb.append(" by request of indexes: [");
            if (unindexedStates != null) {
                for (Pair<FileIndexingStateWithExplanation, ID<?, ?>> state : unindexedStates) {
                    sb.append(state.getSecond()).append("->").append(state.getFirst()).append(",");
                }
            }
            sb.append("]. ");

            if (hasAppliersOrRemovers()) {
                sb.append(getAppliersAndRemoversLogString(indexedFile));
            }
            return sb.toString();
        }
    }

    private static String getLogString(IndexedFile indexedFile) {
        StringBuilder sb = new StringBuilder(indexedFile.getFileName());
        VirtualFile file = indexedFile.getFile();
        if (file instanceof VirtualFileWithId fileWithId) {
            sb.append(" (id=").append(fileWithId.getId()).append(")");
        }
        return sb.toString();
    }

    UnindexedFilesFinder(
        Project project,
        @Nullable BiPredicate<? super IndexedFile, ? super FileIndexingStamp> forceReindexingTrigger,
        ScanningRequestToken indexingRequest
    ) {
        myProject = project;
        myFileBasedIndex = (FileBasedIndexImpl) FileBasedIndex.getInstance();
        // with no explicit trigger the module-aware layer supplies one, so a file whose module options drifted is
        // reindexed by the ordinary scan; it costs one empty-list check when nothing declares option providers
        myForceReindexingTrigger = forceReindexingTrigger != null
            ? forceReindexingTrigger
            : (indexedFile, stamp) -> ModuleAwareIndexMetaRecorder.isOptionsDrifted(indexedFile);
        myIndexingRequest = indexingRequest;
    }

    /**
     * @return null if the file is not subject for indexing (a directory, invalid, etc.)
     */
    public @Nullable UnindexedFileStatus getFileStatus(VirtualFile file) {
        long statusTime = System.nanoTime();
        UnindexedFileStatusBuilder status = evaluateFileStatus(file);
        if (status != null) {
            status.timeTotalEvaluation = System.nanoTime() - statusTime;
            return status.build();
        }
        return null;
    }

    private @Nullable UnindexedFileStatusBuilder evaluateFileStatus(VirtualFile file) {
        if (!file.isValid() || !(file instanceof VirtualFileWithId)) {
            return null;
        }

        // snapshot at the beginning: if file changes while being processed, we can detect this on the following scanning
        FileIndexingStamp indexingStamp = myIndexingRequest.getFileIndexingStamp(file);
        FileIndexingResult.ApplicationMode applicationMode = FileBasedIndexImpl.getContentIndependentIndexesApplicationMode();

        if (IndexingFlag.isFileIndexed(file, indexingStamp) && !shouldForceReindexing(file, indexingStamp)) {
            return new UnindexedFileStatusBuilder(applicationMode);
        }

        Supplier<Boolean> checker = CachedFileType.getFileTypeChangeChecker();
        FileType cachedFileType = file.getFileType();
        return AccessRule.read(() -> {
            if (myProject.isDisposed() || !file.isValid()) {
                return null;
            }

            UnindexedFileStatusBuilder fileStatusBuilder = new UnindexedFileStatusBuilder(applicationMode);

            IndexedFileImpl indexedFile = new IndexedFileImpl(file, checker.get() ? cachedFileType : file.getFileType());
            indexedFile.setProject(myProject);
            int inputId = Math.abs(FileBasedIndexImpl.getIdMaskingNonIdBasedFile(file));

            if (IndexingFlag.isFileIndexed(file, indexingStamp) && !shouldForceReindexing(indexedFile, indexingStamp)) {
                IndexingStamp.flushCache(inputId);
                return fileStatusBuilder;
            }

            SimpleReference<Runnable> finalization = SimpleReference.create();
            FileBasedIndexImpl.getFileTypeManager().freezeFileTypeTemporarilyIn(file, () -> {
                boolean isDirectory = file.isDirectory();
                FileIndexingStateWithExplanation fileTypeIndexState = null;
                boolean shouldCheckContentIndexes;
                if (!isDirectory && !myFileBasedIndex.isTooLarge(file)) {
                    fileTypeIndexState = myFileBasedIndex.getIndexingState(indexedFile, FileTypeIndexImpl.NAME, indexingStamp);
                    if (fileTypeIndexState.isIndexedButOutdated()) {
                        if (myDoTraceForFilesToBeIndexed) {
                            LOG.trace("Scheduling full indexing of " + getLogString(indexedFile) +
                                " because file type index is outdated. " + fileTypeIndexState.getExplanationAsString());
                        }
                        myFileBasedIndex.dropNontrivialIndexedStates(inputId);
                        fileStatusBuilder.shouldIndex = true;
                        shouldCheckContentIndexes = false;
                    }
                    else {
                        shouldCheckContentIndexes = true;
                    }
                }
                else {
                    shouldCheckContentIndexes = false;
                }
                boolean fileTypeIndexAlreadyUpToData = fileTypeIndexState != null && fileTypeIndexState.isUpToDate();
                Set<ID<?, ?>> appliedIndexes = myFileBasedIndex.getAppliedIndexes(inputId);
                List<ID<?, ?>> requiredIndexes = myFileBasedIndex.getRequiredIndexes(indexedFile);

                // TODO - this non-cancelable section is just a precaution, it should be removed once we are sure
                //        the process is cancelled only when expected
                ProgressManager.getInstance().executeNonCancelableSection(() -> {
                    for (ID<?, ?> indexId : requiredIndexes) {
                        appliedIndexes.remove(indexId);

                        boolean needsFileContentLoading = myFileBasedIndex.needsFileContentLoading(indexId);
                        // this is the same: (shouldCheckContentIndexes && needsFileContentLoading) || !needsFileContentLoading
                        boolean shouldCheckAgainstSingleIndex = !needsFileContentLoading || shouldCheckContentIndexes;

                        // if FileTypeIndex already checked, no need to check it twice
                        if (shouldCheckAgainstSingleIndex && !(FileTypeIndexImpl.NAME.equals(indexId) && fileTypeIndexAlreadyUpToData)) {
                            // measure contentless indexes only
                            long contentlessStartTime = needsFileContentLoading ? -1 : System.nanoTime();
                            try {
                                applyOrScheduleRequiredIndex(indexId, fileStatusBuilder, indexedFile, inputId, indexingStamp);
                            }
                            finally {
                                if (contentlessStartTime >= 0) {
                                    fileStatusBuilder.timeUpdatingContentLessIndexes += (System.nanoTime() - contentlessStartTime);
                                }
                            }
                        }
                    }

                    // remove unneeded data from indexes
                    for (ID<?, ?> indexId : appliedIndexes) {
                        removeIndexedValue(indexedFile, inputId, indexId, fileStatusBuilder);
                    }

                    if (!fileStatusBuilder.hasAppliersOrRemovers()) {
                        finishGettingStatus(file, indexedFile, inputId, fileStatusBuilder, indexingStamp);
                        finalization.set(EmptyRunnable.INSTANCE);
                    }
                    else {
                        finalization.set(() -> {
                            long applyingStart = System.nanoTime();
                            try {
                                for (SingleIndexValueRemover remover : fileStatusBuilder.removers) {
                                    remover.remove();
                                }
                                for (SingleIndexValueApplier applier : fileStatusBuilder.appliers) {
                                    applier.apply();
                                }
                            }
                            finally {
                                fileStatusBuilder.timeUpdatingContentLessIndexes += (System.nanoTime() - applyingStart);
                            }
                            finishGettingStatus(file, indexedFile, inputId, fileStatusBuilder, indexingStamp);
                        });
                    }
                });
            });

            finalization.get().run();

            if (myDoTraceForFilesToBeIndexed) {
                fileStatusBuilder.explain(indexedFile);
            }
            return fileStatusBuilder;
        });
    }

    private void applyOrScheduleRequiredIndex(
        ID<?, ?> indexId,
        UnindexedFileStatusBuilder fileStatusBuilder,
        IndexedFileImpl indexedFile,
        int inputId,
        FileIndexingStamp indexingStamp
    ) {
        if (!RebuildStatus.isOk(indexId)) {
            fileStatusBuilder.mayMarkFileIndexed = false;
            return;
        }

        try {
            FileIndexingStateWithExplanation fileIndexingState = myFileBasedIndex.getIndexingState(indexedFile, indexId, indexingStamp);
            if (fileIndexingState.updateRequired()) {
                if (myDoTraceForFilesToBeIndexed) {
                    LOG.trace(
                        "Scheduling indexing of " + getLogString(indexedFile) + " by request of index " + indexId + ";" +
                            "indexing state = " + fileIndexingState
                    );
                }

                fileStatusBuilder.addUnindexedState(fileIndexingState, indexId);

                if (!tryIndexWithoutContent(indexedFile, inputId, indexId, fileStatusBuilder)) {
                    fileStatusBuilder.shouldIndex = true;
                }
            }
        }
        catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException || cause instanceof StorageException) {
                LOG.info(e);
                myFileBasedIndex.requestRebuild(indexId, cause);
            }
            else {
                throw e;
            }
        }
    }

    private void removeIndexedValue(
        IndexedFileImpl indexedFile,
        int inputId,
        ID<?, ?> indexId,
        UnindexedFileStatusBuilder fileStatusBuilder
    ) {
        SingleIndexValueRemover remover = myFileBasedIndex.createSingleIndexRemover(
            indexId,
            indexedFile.getFile(),
            null,
            inputId,
            fileStatusBuilder.applicationMode
        );
        if (remover != null) {
            boolean removed = fileStatusBuilder.addOrRunRemover(remover);
            if (!removed) {
                LOG.error("Failed to remove value from index " + indexId + " for file " + indexedFile.getFile() + ", " +
                    "applicationMode=" + fileStatusBuilder.applicationMode);
            }
        }
    }

    private boolean tryIndexWithoutContent(
        IndexedFileImpl indexedFile,
        int inputId,
        ID<?, ?> indexId,
        UnindexedFileStatusBuilder fileStatusBuilder
    ) {
        if (myFileBasedIndex.needsFileContentLoading(indexId)) {
            return false;
        }

        FileContentImpl fileContent = new FileContentImpl(indexedFile.getFile());
        fileContent.setProject(indexedFile.getProject());
        SingleIndexValueApplier applier =
            myFileBasedIndex.createSingleIndexValueApplier(indexId, indexedFile.getFile(), inputId, fileContent);
        if (applier != null) {
            boolean updated = fileStatusBuilder.addOrRunApplier(applier);
            if (!updated) {
                LOG.error("Failed to apply contentless indexer " + indexId + " to file " + indexedFile.getFile() + ", " +
                    "applicationMode =" + fileStatusBuilder.applicationMode);
            }
            return updated;
        }
        return true;
    }

    private void finishGettingStatus(
        VirtualFile file,
        IndexedFileImpl indexedFile,
        int inputId,
        UnindexedFileStatusBuilder fileStatusBuilder,
        FileIndexingStamp indexingStamp
    ) {
        if (shouldForceReindexing(indexedFile, indexingStamp)) {
            myFileBasedIndex.dropNontrivialIndexedStates(inputId);
            fileStatusBuilder.shouldIndex = true;
        }

        IndexingStamp.flushCache(inputId);
        if (!fileStatusBuilder.shouldIndex && fileStatusBuilder.mayMarkFileIndexed) {
            IndexingFlag.setFileIndexed(file, indexingStamp);
        }
    }

    /**
     * Returns whether the trigger requires reindexing, avoiding an {@link IndexedFile} allocation when no trigger is configured.
     */
    private boolean shouldForceReindexing(VirtualFile vFile, FileIndexingStamp indexingStamp) {
        if (myForceReindexingTrigger == null) {
            return false;
        }
        IndexedFileImpl indexedFile = new IndexedFileImpl(vFile, vFile.getFileType());
        indexedFile.setProject(myProject);
        return shouldForceReindexing(indexedFile, indexingStamp);
    }

    /**
     * Returns whether the trigger requires reindexing regardless of the file's current indexing status.
     */
    private boolean shouldForceReindexing(IndexedFile indexedFile, FileIndexingStamp indexingStamp) {
        return myForceReindexingTrigger != null && myForceReindexingTrigger.test(indexedFile, indexingStamp);
    }
}

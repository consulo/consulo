// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.index.io.ID;
import consulo.index.io.IndexId;
import consulo.language.index.impl.internal.hints.AcceptAllFilesAndDirectoriesIndexingHint;
import consulo.language.index.impl.internal.hints.AcceptAllRegularFilesIndexingHint;
import consulo.language.index.impl.internal.hints.BaseGlobalFileTypeInputFilter;
import consulo.language.index.impl.internal.hints.FileTypeIndexingHint;
import consulo.language.index.impl.internal.hints.FileTypeInputFilterPredicate;
import consulo.language.index.impl.internal.hints.GlobalIndexSpecificIndexingHint;
import consulo.language.index.impl.internal.hints.RejectAllIndexingHint;
import consulo.language.internal.SubstitutedFileType;
import consulo.language.psi.search.FileTypeIndex;
import consulo.language.psi.stub.DefaultFileTypeSpecificInputFilter;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.IndexedFile;
import consulo.logging.Logger;
import consulo.util.lang.Pair;
import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Evaluates indexes applicable for a particular file or file type
 */
final class RequiredIndexesEvaluator {
    private static final Logger LOG = Logger.getInstance(RequiredIndexesEvaluator.class);

    private interface IndexedFilePredicate extends Predicate<IndexedFile> {
    }

    private final IndexedFilePredicate myTruePredicate = t -> true;

    private final IndexedFilePredicate myFalsePredicate = t -> false;

    private final IndexConfiguration myState;

    private final Map<FileType, HintAwareIndexList> myIndexesForFileType = new ConcurrentHashMap<>();
    private final HintAwareIndexList myIndexesForDirectories;

    /**
     * Indexes whose input filter has already been reported as broken: each one is reported only once, to not flood the log.
     */
    private final Set<ID<?, ?>> myBrokenInputFiltersReported = ConcurrentHashMap.newKeySet();

    RequiredIndexesEvaluator(IndexConfiguration state, Collection<ID<?, ?>> indicesForDirectories) {
        myState = state;
        myIndexesForDirectories = indexesForDirectories(indicesForDirectories);
    }

    private IndexedFilePredicate booleanToIndexedFilePredicate(boolean b) {
        return b ? myTruePredicate : myFalsePredicate;
    }

    private IndexedFilePredicate andPredicates(IndexedFilePredicate p1, IndexedFilePredicate p2) {
        if (p1 == myFalsePredicate) {
            return p1;
        }
        else if (p2 == myFalsePredicate) {
            return p2;
        }
        else if (p1 == myTruePredicate) {
            return p2;
        }
        else if (p2 == myTruePredicate) {
            return p1;
        }
        else {
            return t -> p1.test(t) && p2.test(t);
        }
    }

    private HintAwareIndexList indexesForDirectories(Collection<ID<?, ?>> indexIds) {
        return buildHintAwareIndexList("Directories", indexIds, this::acceptDirectory);
    }

    private HintAwareIndexList indexesForRegularFiles(Collection<ID<?, ?>> indexIds, FileType fileType) {
        return buildHintAwareIndexList("FileType " + fileType, indexIds, indexId -> acceptRegularFile(indexId, fileType));
    }

    private HintAwareIndexList buildHintAwareIndexList(
        String listDebugName,
        Collection<ID<?, ?>> indexIds,
        Function<ID<?, ?>, IndexedFilePredicate> indexToPredicate
    ) {
        List<ID<?, ?>> sure = new ArrayList<>();
        List<Pair<ID<?, ?>, IndexedFilePredicate>> unsure = new ArrayList<>();
        for (ID<?, ?> indexId : indexIds) {
            IndexedFilePredicate predicate = indexToPredicate.apply(indexId);
            if (predicate == myTruePredicate) {
                sure.add(indexId);
            }
            else if (predicate != myFalsePredicate) {
                unsure.add(Pair.create(indexId, predicate));
            }
        }

        if (LOG.isDebugEnabled()) {
            List<String> allNames = new ArrayList<>();
            for (ID<?, ?> id : sure) {
                allNames.add(id.getName());
            }
            for (Pair<ID<?, ?>, IndexedFilePredicate> pair : unsure) {
                allNames.add(pair.getFirst().getName());
            }
            LOG.debug(listDebugName + ", will be indexed by: " + allNames);

            if (!unsure.isEmpty()) {
                List<String> unsureNames = new ArrayList<>();
                for (Pair<ID<?, ?>, IndexedFilePredicate> pair : unsure) {
                    unsureNames.add(pair.getFirst().getName());
                }
                LOG.debug(listDebugName + ", slow scanning path via indexes: " + unsureNames);
            }
        }

        return new HintAwareIndexList(sure, unsure);
    }

    private final class HintAwareIndexList {
        private final List<ID<?, ?>> mySureIndexIds;
        private final List<Pair<ID<?, ?>, IndexedFilePredicate>> myUnsureIndexIds;

        private HintAwareIndexList(List<ID<?, ?>> sureIndexIds, List<Pair<ID<?, ?>, IndexedFilePredicate>> unsureIndexIds) {
            mySureIndexIds = sureIndexIds;
            myUnsureIndexIds = unsureIndexIds;
        }

        List<ID<?, ?>> getRequiredIndexes(IndexedFile indexedFile) {
            if (myUnsureIndexIds.isEmpty()) {
                return mySureIndexIds;
            }

            // IDEA-320788: this assertion is not correct. Currently, the project can be null in the following cases:
            //   1. VFS refreshed a file before scanning added it to per-project indexable files holder
            //   2. VFS refreshed a file that belonged to a project that already closed
            //   3. VFS refreshed a file that belongs to opened project, but excluded
            List<ID<?, ?>> acceptedCandidates = new ArrayList<>(mySureIndexIds);
            for (Pair<ID<?, ?>, IndexedFilePredicate> pair : myUnsureIndexIds) {
                if (acceptsInputSafely(pair.getFirst(), pair.getSecond(), indexedFile)) {
                    acceptedCandidates.add(pair.getFirst());
                }
            }
            return acceptedCandidates;
        }

        List<ID<?, ?>> getSureIndexes() {
            return mySureIndexIds;
        }

        List<ID<?, ?>> getUnsureIndexes() {
            List<ID<?, ?>> result = new ArrayList<>();
            for (Pair<ID<?, ?>, IndexedFilePredicate> pair : myUnsureIndexIds) {
                result.add(pair.getFirst());
            }
            return result;
        }
    }

    /**
     * Asks the index's input filter if it accepts the file, tolerating a filter that fails.
     * A filter is a plugin's code, and it can throw anything.
     *
     * @return true if the index accepts the file, false if it doesn't -- or if we can't tell because the filter failed
     */
    private boolean acceptsInputSafely(ID<?, ?> indexId, Predicate<IndexedFile> filter, IndexedFile indexedFile) {
        try {
            return filter.test(indexedFile);
        }
        catch (Throwable t) {
            String message = "Index '" + indexId.getName() + "': input filter failed on '" + indexedFile.getFileName() + "', " +
                "the index is skipped for this file";
            if (myBrokenInputFiltersReported.add(indexId)) {
                LOG.error(message + ". Files will keep being skipped by this index, so the plugin is not fully functional. " +
                    "This is reported only once.", t);
            }
            else if (LOG.isDebugEnabled()) {
                LOG.debug(message, t);
            }
            return false;
        }
    }

    private FileBasedIndex.InputFilter getInputFilter(ID<?, ?> indexId) {
        return myState.getInputFilter(indexId);
    }

    private IndexedFilePredicate inputFilerToIndexedFilePredicateForRegularFile(
        FileBasedIndex.InputFilter inputFilter,
        FileType fileType
    ) {
        FileTypeIndexingHint hint = toHint(inputFilter);
        if (hint != null) {
            return applyFileTypeHints(hint, fileType);
        }
        return inputFilerToIndexedFilePredicate(inputFilter, false);
    }

    private IndexedFilePredicate inputFilerToIndexedFilePredicate(FileBasedIndex.InputFilter inputFilter, boolean isDirectory) {
        if (inputFilter == RejectAllIndexingHint.INSTANCE) {
            return myFalsePredicate;
        }
        if (inputFilter == AcceptAllRegularFilesIndexingHint.INSTANCE) {
            return booleanToIndexedFilePredicate(!isDirectory);
        }
        if (inputFilter == AcceptAllFilesAndDirectoriesIndexingHint.INSTANCE) {
            return myTruePredicate;
        }
        return indexedFile -> inputFilter.acceptInput(indexedFile.getProject(), indexedFile.getFile());
    }

    private IndexedFilePredicate acceptDirectory(ID<?, ?> indexId) {
        FileBasedIndex.InputFilter inputFilter = getInputFilter(indexId);

        IndexedFilePredicate indexerHintPredicate = inputFilerToIndexedFilePredicate(inputFilter, true);
        IndexedFilePredicate globalHintPredicate = getGlobalIndexedFilePredicateForDirectory(indexId);

        return andPredicates(indexerHintPredicate, globalHintPredicate);
    }

    private IndexedFilePredicate acceptRegularFile(ID<?, ?> indexId, FileType fileType) {
        FileBasedIndex.InputFilter inputFilter = getInputFilter(indexId);

        IndexedFilePredicate indexerHintPredicate = inputFilerToIndexedFilePredicateForRegularFile(inputFilter, fileType);
        IndexedFilePredicate globalHintPredicate = getGlobalIndexedFilePredicateForRegularFile(indexId, fileType);

        return andPredicates(indexerHintPredicate, globalHintPredicate);
    }

    private @Nullable FileTypeIndexingHint toHint(FileBasedIndex.InputFilter filter) {
        if (filter instanceof FileTypeIndexingHint fileTypeIndexingHint) {
            return fileTypeIndexingHint;
        }
        // yes, we want to check exact class.
        // Optimization does not work for DefaultFileTypeSpecificInputFilter subtypes because subtypes can override acceptInput
        if (filter instanceof DefaultFileTypeSpecificInputFilter defaultFilter
            && filter.getClass() == DefaultFileTypeSpecificInputFilter.class) {
            return new FileTypeInputFilterPredicate(fileType -> {
                boolean[] matches = new boolean[]{false};
                defaultFilter.registerFileTypesUsedForIndexing(it -> matches[0] = matches[0] || it == fileType);
                return matches[0];
            });
        }
        return null;
    }

    private IndexedFilePredicate getGlobalIndexedFilePredicateForDirectory(IndexId<?, ?> indexId) {
        IndexedFilePredicate allGlobalHints = myTruePredicate;
        for (GlobalIndexFilter filter : GlobalIndexFilter.EP_NAME.getExtensionList()) {
            IndexedFilePredicate hintPredicate;
            if (filter instanceof BaseGlobalFileTypeInputFilter globalFileTypeInputFilter) {
                hintPredicate = booleanToIndexedFilePredicate(globalFileTypeInputFilter.isAcceptsDirectories());
            }
            else {
                hintPredicate = globalFilterToIndexedFilePredicate(filter, indexId);
            }
            allGlobalHints = andPredicates(allGlobalHints, hintPredicate);
        }
        return allGlobalHints;
    }

    private IndexedFilePredicate getGlobalIndexedFilePredicateForRegularFile(IndexId<?, ?> indexId, FileType fileType) {
        IndexedFilePredicate allGlobalHints = myTruePredicate;
        for (GlobalIndexFilter filter : GlobalIndexFilter.EP_NAME.getExtensionList()) {
            FileBasedIndex.InputFilter inputFilter = filter instanceof GlobalIndexSpecificIndexingHint hint
                ? hint.globalInputFilterForIndex(indexId)
                : null;
            IndexedFilePredicate hintPredicate = inputFilter != null
                ? inputFilerToIndexedFilePredicateForRegularFile(inputFilter, fileType)
                : globalFilterToIndexedFilePredicate(filter, indexId);
            allGlobalHints = andPredicates(allGlobalHints, hintPredicate);
        }

        return allGlobalHints;
    }

    private IndexedFilePredicate globalFilterToIndexedFilePredicate(GlobalIndexFilter filter, IndexId<?, ?> indexId) {
        return indexedFile -> !filter.isExcludedFromIndex(indexedFile.getFile(), indexId);
    }

    private IndexedFilePredicate applyFileTypeHints(FileTypeIndexingHint indexingHint, FileType fileType) {
        ThreeState state = indexingHint.acceptsFileTypeFastPath(fileType);
        return switch (state) {
            case YES -> myTruePredicate;
            case NO -> myFalsePredicate;
            case UNSURE -> indexingHint::slowPathIfFileTypeHintUnsure;
        };
    }

    List<ID<?, ?>> getRequiredIndexes(IndexedFile indexedFile) {
        if (indexedFile.getFile().isDirectory()) {
            return getRequiredIndexesForDirectories(indexedFile);
        }
        return getRequiredIndexesForRegularFiles(indexedFile);
    }

    private List<ID<?, ?>> getRequiredIndexesForRegularFiles(IndexedFile indexedFile) {
        FileType fileType = indexedFile.getFileType();
        FileType substitutedFileType = fileType instanceof SubstitutedFileType substituted ? substituted.getFileType() : fileType;

        if (FileBasedIndexImpl.isProjectOrWorkspaceFile(indexedFile.getFile(), substitutedFileType)) {
            return List.<ID<?, ?>>of(FileTypeIndex.NAME); // probably, we don't even need the filetype index
        }
        return getIndexesForFileType(fileType).getRequiredIndexes(indexedFile);
    }

    private HintAwareIndexList getIndexesForFileType(FileType fileType) {
        return myIndexesForFileType.computeIfAbsent(fileType, ft -> {
            FileType substitutedFileType = ft instanceof SubstitutedFileType substituted ? substituted.getFileType() : ft;
            return indexesForRegularFiles(myState.getFileTypesForIndex(substitutedFileType), ft);
        });
    }

    private List<ID<?, ?>> getRequiredIndexesForDirectories(IndexedFile indexedFile) {
        if (FileBasedIndexImpl.isProjectOrWorkspaceFile(indexedFile.getFile(), null)) {
            return Collections.emptyList();
        }
        return myIndexesForDirectories.getRequiredIndexes(indexedFile);
    }

    Pair<List<ID<?, ?>>, List<ID<?, ?>>> getRequiredIndexesForFileType(FileType fileType) {
        HintAwareIndexList indexes = getIndexesForFileType(fileType);
        return Pair.create(indexes.getSureIndexes(), indexes.getUnsureIndexes());
    }
}

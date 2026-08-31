/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.data;

import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.log.graph.GraphCommit;
import it.unimi.dsi.fastutil.ints.*;

import java.util.*;

/**
 * Attaches the block of the latest commits, which was read from the VCS, to the existing log structure.
 *
 * @author Stanislav Erokhin
 * @author Kirill Likhodedov
 */
public final class VcsLogJoiner<CommitId, Commit extends GraphCommit<CommitId>> {
    public static final String ILLEGAL_DATA_RELOAD_ALL = "All data is illegal - request reload all";

    // Lets newSet(...) use a primitive IntOpenHashSet for Integer ids (less boxing/memory)
    private final Class<CommitId> myCommitIdClass;

    public VcsLogJoiner(Class<CommitId> commitIdClass) {
        myCommitIdClass = commitIdClass;
    }

    /**
     * Attaches the block of the latest commits, which was read from the VCS, to the existing log structure.
     *
     * @param savedLog     currently available part of the log.
     * @param previousRefs references saved from the previous refresh.
     * @param firstBlock   the first n commits read from the VCS.
     * @param newRefs      all references (branches) of the repository.
     * @return Total saved log with new commits properly attached to it + number of new commits attached to the log.
     */
    public Pair<List<? extends Commit>, Integer> addCommits(
        List<? extends Commit> savedLog,
        Collection<CommitId> previousRefs,
        List<? extends Commit> firstBlock,
        Collection<CommitId> newRefs
    ) {
        Pair<Integer, Set<Commit>> newCommitsAndSavedGreenIndex =
            getNewCommitsAndSavedGreenIndex(savedLog, previousRefs, firstBlock, newRefs);
        Pair<Integer, Set<CommitId>> redCommitsAndSavedRedIndex =
            getRedCommitsAndSavedRedIndex(savedLog, previousRefs, firstBlock, newRefs);

        Set<CommitId> removeCommits = redCommitsAndSavedRedIndex.second;
        Set<Commit> allNewCommits = newCommitsAndSavedGreenIndex.second;

        if (removeCommits.isEmpty() && allNewCommits.isEmpty()) {
            return Pair.create(savedLog, 0);
        }

        int unsafeBlockSize = Math.max(redCommitsAndSavedRedIndex.first, newCommitsAndSavedGreenIndex.first);
        List<Commit> unsafePartSavedLog = new ArrayList<>();
        for (Commit commit : savedLog.subList(0, unsafeBlockSize)) {
            if (!removeCommits.contains(commit.getId())) {
                unsafePartSavedLog.add(commit);
            }
        }
        unsafePartSavedLog = new NewCommitIntegrator<>(unsafePartSavedLog, allNewCommits).getResultList();

        return Pair.create(
            ContainerUtil.concat(unsafePartSavedLog, savedLog.subList(unsafeBlockSize, savedLog.size())),
            unsafePartSavedLog.size() - unsafeBlockSize
        );
    }

    private Pair<Integer, Set<Commit>> getNewCommitsAndSavedGreenIndex(
        List<? extends Commit> savedLog,
        Collection<CommitId> previousRefs,
        List<? extends Commit> firstBlock,
        Collection<CommitId> newRefs
    ) {
        Set<CommitId> allUnresolvedLinkedHashes = newSet(newRefs);
        allUnresolvedLinkedHashes.removeAll(previousRefs);
        // at this moment allUnresolvedLinkedHashes contains only NEW refs
        for (Commit commit : firstBlock) {
            allUnresolvedLinkedHashes.add(commit.getId());
            allUnresolvedLinkedHashes.addAll(commit.getParents());
        }
        for (Commit commit : firstBlock) {
            if (!commit.getParents().isEmpty()) {
                allUnresolvedLinkedHashes.remove(commit.getId());
            }
        }
        int saveGreenIndex = getFirstUnTrackedIndex(savedLog, allUnresolvedLinkedHashes);

        return new Pair<>(saveGreenIndex, getAllNewCommits(savedLog.subList(0, saveGreenIndex), firstBlock));
    }

    private int getFirstUnTrackedIndex(List<? extends Commit> commits, Set<CommitId> searchHashes) {
        int lastIndex;
        for (lastIndex = 0; lastIndex < commits.size(); lastIndex++) {
            Commit commit = commits.get(lastIndex);
            if (searchHashes.isEmpty()) {
                return lastIndex;
            }
            searchHashes.remove(commit.getId());
        }
        if (!searchHashes.isEmpty()) {
            throw new VcsLogRefreshNotEnoughDataException();
        }
        return lastIndex;
    }

    private Set<Commit> getAllNewCommits(List<? extends Commit> unsafeGreenPartSavedLog, List<? extends Commit> firstBlock) {
        Set<CommitId> existedCommitHashes = newSet();
        for (Commit commit : unsafeGreenPartSavedLog) {
            existedCommitHashes.add(commit.getId());
        }
        Set<Commit> allNewsCommits = new HashSet<>();
        for (Commit newCommit : firstBlock) {
            if (!existedCommitHashes.contains(newCommit.getId())) {
                allNewsCommits.add(newCommit);
            }
        }
        return allNewsCommits;
    }

    private Pair<Integer, Set<CommitId>> getRedCommitsAndSavedRedIndex(
        List<? extends Commit> savedLog,
        Collection<CommitId> previousRefs,
        List<? extends Commit> firstBlock,
        Collection<CommitId> newRefs
    ) {
        Set<CommitId> startRedCommits = newSet(previousRefs);
        startRedCommits.removeAll(newRefs);
        Set<CommitId> startGreenNodes = newSet(newRefs);
        for (Commit commit : firstBlock) {
            startGreenNodes.add(commit.getId());
            startGreenNodes.addAll(commit.getParents());
        }
        RedGreenSorter sorter = new RedGreenSorter(startRedCommits, startGreenNodes, savedLog);
        int saveRegIndex = sorter.getFirstSaveIndex();

        return new Pair<>(saveRegIndex, sorter.getAllRedCommit());
    }

    @SuppressWarnings("unchecked")
    private Set<CommitId> newSet() {
        if (myCommitIdClass == Integer.class) {
            return (Set<CommitId>) new IntOpenHashSet();
        }
        return new HashSet<>();
    }

    @SuppressWarnings("unchecked")
    private Set<CommitId> newSet(Collection<CommitId> contents) {
        if (myCommitIdClass == Integer.class) {
            return (Set<CommitId>) new IntOpenHashSet((Collection<Integer>) contents);
        }
        return new HashSet<>(contents);
    }

    private final class RedGreenSorter {
        private final Set<CommitId> currentRed;
        private final Set<CommitId> currentGreen;
        private final Set<CommitId> allRedCommit = newSet();

        private final List<? extends Commit> savedLog;

        private RedGreenSorter(Set<CommitId> startRedNodes, Set<CommitId> startGreenNodes, List<? extends Commit> savedLog) {
            this.currentRed = startRedNodes;
            this.currentGreen = startGreenNodes;
            this.savedLog = savedLog;
        }

        private void markRealRedNode(CommitId node) {
            if (!currentRed.remove(node)) {
                throw new IllegalStateException(ILLEGAL_DATA_RELOAD_ALL); // see VcsLogJoinerTest#illegalStateExceptionTest2
            }
            allRedCommit.add(node);
        }

        private int getFirstSaveIndex() {
            for (int lastIndex = 0; lastIndex < savedLog.size(); lastIndex++) {
                Commit commit = savedLog.get(lastIndex);
                boolean isGreen = currentGreen.contains(commit.getId());
                if (isGreen) {
                    currentRed.remove(commit.getId());
                    currentGreen.addAll(commit.getParents());
                }
                else {
                    markRealRedNode(commit.getId());
                    currentRed.addAll(commit.getParents());
                }

                if (currentRed.isEmpty()) {
                    return lastIndex + 1;
                }
            }
            throw new IllegalStateException(ILLEGAL_DATA_RELOAD_ALL); // see VcsLogJoinerTest#illegalStateExceptionTest
        }

        public Set<CommitId> getAllRedCommit() {
            return allRedCommit;
        }
    }

    static class NewCommitIntegrator<CommitId, Commit extends GraphCommit<CommitId>> {
        private final List<Commit> list;
        private final Map<CommitId, Commit> newCommitsMap;

        private final Stack<Commit> commitsStack;

        NewCommitIntegrator(List<Commit> list, Collection<Commit> newCommits) {
            this.list = list;
            newCommitsMap = new HashMap<>();
            for (Commit commit : newCommits) {
                newCommitsMap.put(commit.getId(), commit);
            }
            commitsStack = new Stack<>();
        }

        private void insertAllUseStack() {
            while (!newCommitsMap.isEmpty()) {
                visitCommit(Objects.requireNonNull(ContainerUtil.getFirstItem(newCommitsMap.values())));
                while (!commitsStack.isEmpty()) {
                    Commit currentCommit = commitsStack.peek();
                    boolean allParentsWereAdded = true;
                    for (CommitId parentHash : currentCommit.getParents()) {
                        Commit parentCommit = newCommitsMap.get(parentHash);
                        if (parentCommit != null) {
                            visitCommit(parentCommit);
                            allParentsWereAdded = false;
                            break;
                        }
                    }

                    if (!allParentsWereAdded) {
                        continue;
                    }

                    int insertIndex;
                    Set<CommitId> parents = new HashSet<>(currentCommit.getParents());
                    for (insertIndex = 0; insertIndex < list.size(); insertIndex++) {
                        Commit someCommit = list.get(insertIndex);
                        if (parents.contains(someCommit.getId())) {
                            break;
                        }
                        if (someCommit.getTimestamp() < currentCommit.getTimestamp()) {
                            break;
                        }
                    }

                    list.add(insertIndex, currentCommit);
                    commitsStack.pop();
                }
            }
        }

        private void visitCommit(Commit commit) {
            commitsStack.push(commit);
            newCommitsMap.remove(commit.getId());
        }

        public List<Commit> getResultList() {
            insertAllUseStack();
            return list;
        }
    }
}

// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.versionControlSystem.log.base;

import consulo.application.progress.ProgressManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.versionControlSystem.change.Change;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

public class VcsChangesLazilyParsedDetailsUnparsedChanges implements VcsChangesLazilyParsedDetailsChanges {
    private static final Logger LOG = Logger.getInstance(VcsChangesLazilyParsedDetails.class);

    protected final Project myProject;
    protected final List<List<VcsFileStatusInfo>> myChangesOutput;
    private final int myParentsCount;
    private final VcsStatusMerger<VcsFileStatusInfo, CharSequence> myStatusMerger = new VcsFileStatusInfoMerger();
    private final BiFunction<List<VcsFileStatusInfo>, Integer, List<Change>> myParser;
    private final AtomicReference<VcsChangesLazilyParsedDetailsChanges> myOwner;

    public VcsChangesLazilyParsedDetailsUnparsedChanges(Project project,
                                                        List<List<VcsFileStatusInfo>> changesOutput,
                                                        int parentsCount,
                                                        BiFunction<List<VcsFileStatusInfo>, Integer, List<Change>> parser,
                                                        AtomicReference<VcsChangesLazilyParsedDetailsChanges> owner) {
        myProject = project;
        myChangesOutput = changesOutput;
        myParentsCount = parentsCount;
        myParser = parser;
        myOwner = owner;
    }

    protected VcsChangesLazilyParsedDetailsParsedChanges parseChanges() {
        List<Change> mergedChanges = parseMergedChanges();
        List<Collection<Change>> changes = computeChanges(mergedChanges);
        VcsChangesLazilyParsedDetailsParsedChanges parsedChanges = new VcsChangesLazilyParsedDetailsParsedChanges(mergedChanges, changes);
        myOwner.compareAndSet(this, parsedChanges);
        return parsedChanges;
    }

    private List<Change> parseMergedChanges() {
        List<MergedStatusInfo<VcsFileStatusInfo>> statuses = getMergedStatusInfo();
        List<Change> changes = myParser.apply(statuses.stream().map(MergedStatusInfo::getStatusInfo).toList(), 0);
        if (changes.size() != statuses.size()) {
            LOG.error("Incorrectly parsed statuses " + statuses + " to changes " + changes);
        }
        if (myParentsCount <= 1) {
            return changes;
        }

        // each merge change knows about all changes to parents
        List<Change> wrappedChanges = new ArrayList<>(statuses.size());
        for (int i = 0; i < statuses.size(); i++) {
            wrappedChanges.add(new VcsChangesLazilyParsedDetailsMergedChange(changes.get(i), statuses.get(i), myParser));
        }
        return wrappedChanges;
    }

    @Override
    public Collection<Change> getMergedChanges() {
        return parseChanges().getMergedChanges();
    }

    @Override
    public Collection<Change> getChanges(int parent) {
        return parseChanges().getChanges(parent);
    }

    @Override
    public int size() {
        int size = 0;
        for (List<VcsFileStatusInfo> changesToParent : myChangesOutput) {
            size += changesToParent.size();
        }
        return size;
    }

    private List<Collection<Change>> computeChanges(Collection<Change> mergedChanges) {
        if (myChangesOutput.size() == 1) {
            return Collections.singletonList(mergedChanges);
        }
        else {
            List<Collection<Change>> changes = new ArrayList<>(myChangesOutput.size());
            for (int i = 0; i < myChangesOutput.size(); i++) {
                ProgressManager.checkCanceled();
                changes.add(myParser.apply(myChangesOutput.get(i), i));
            }
            return changes;
        }
    }

    /*
     * This method mimics result of `-c` option added to `git log` command.
     * It calculates statuses for files that were modified in all parents of a merge commit.
     * If a commit is not a merge, all statuses are returned.
     */
    private List<MergedStatusInfo<VcsFileStatusInfo>> getMergedStatusInfo() {
        return myStatusMerger.merge(myChangesOutput);
    }

    public Collection<VcsFileStatusInfo> getMergedStatuses() {
        Collection<VcsFileStatusInfo> result = new HashSet<>();
        for (MergedStatusInfo<VcsFileStatusInfo> mergedStatusInfo : getMergedStatusInfo()) {
            result.add(mergedStatusInfo.getStatusInfo());
        }
        return result;
    }
}

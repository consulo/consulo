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

import consulo.project.Project;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.log.Hash;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import consulo.versionControlSystem.log.VcsUser;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Allows to postpone changes parsing, which might take long for a large amount of commits,
 * because {@link Change} holds {@code LocalFilePath} which makes costly refreshes and type detections.
 */
public class VcsChangesLazilyParsedDetails extends VcsCommitMetadataImpl implements VcsFullCommitDetails {
    protected static final VcsChangesLazilyParsedDetailsChanges EMPTY_CHANGES = new VcsChangesLazilyParsedDetailsEmptyChanges();

    private final VcsChangesLazilyParsedDetailsChangesParser myChangesParser;
    private final AtomicReference<VcsChangesLazilyParsedDetailsChanges> myChanges = new AtomicReference<>();

    public VcsChangesLazilyParsedDetails(Project project, Hash hash, List<Hash> parents, long commitTime,
                                         VirtualFile root,
                                         String subject, VcsUser author, String message,
                                         VcsUser committer, long authorTime,
                                         List<List<VcsFileStatusInfo>> reportedChanges,
                                         VcsChangesLazilyParsedDetailsChangesParser changesParser) {
        super(hash, parents, commitTime, root, subject, author, message, committer, authorTime);
        myChangesParser = changesParser;
        myChanges.set(reportedChanges.isEmpty() ? EMPTY_CHANGES : new VcsChangesLazilyParsedDetailsUnparsedChanges(
            project,
            reportedChanges,
            getParents().size(),
            (sources, parent) -> myChangesParser.parseStatusInfo(project, this, sources, parent),
            myChanges
        ));
    }

    @Override
    public Collection<Change> getChanges() {
        return myChanges.get().getMergedChanges();
    }

    @Override
    public Collection<Change> getChanges(int parent) {
        return myChanges.get().getChanges(parent);
    }

    public int size() {
        return myChanges.get().size();
    }

    protected VcsChangesLazilyParsedDetailsChanges getChangesObject() {
        return myChanges.get();
    }
}

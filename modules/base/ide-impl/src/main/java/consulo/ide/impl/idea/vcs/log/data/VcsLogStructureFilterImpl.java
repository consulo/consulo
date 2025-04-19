/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.vcs.log.data;

import consulo.util.collection.ContainerUtil;
import consulo.util.io.FileUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.log.VcsCommitMetadata;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import consulo.versionControlSystem.log.VcsLogDetailsFilter;
import consulo.versionControlSystem.log.VcsLogStructureFilter;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Set;

public class VcsLogStructureFilterImpl implements VcsLogDetailsFilter, VcsLogStructureFilter {
    @Nonnull
    private final Collection<FilePath> myFiles;

    public VcsLogStructureFilterImpl(@Nonnull Set<VirtualFile> files) {
        this(ContainerUtil.map(files, VcsUtil::getFilePath));
    }

    public VcsLogStructureFilterImpl(@Nonnull Collection<FilePath> files) {
        myFiles = files;
    }

    @Nonnull
    @Override
    public Collection<FilePath> getFiles() {
        return myFiles;
    }

    @Override
    public boolean matches(@Nonnull VcsCommitMetadata details) {
        if (details instanceof VcsFullCommitDetails vcsFullCommitDetails) {
            for (Change change : vcsFullCommitDetails.getChanges()) {
                ContentRevision before = change.getBeforeRevision();
                if (before != null && matches(before.getFile().getPath())) {
                    return true;
                }
                ContentRevision after = change.getAfterRevision();
                if (after != null && matches(after.getFile().getPath())) {
                    return true;
                }
            }
            return false;
        }
        else {
            return false;
        }
    }

    private boolean matches(@Nonnull String path) {
        return ContainerUtil.find(myFiles, file -> FileUtil.isAncestor(file.getPath(), path, false)) != null;
    }
}

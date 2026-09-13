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

import consulo.content.ContentIterator;
import consulo.language.index.impl.internal.roots.IndexableFilesIterationMethods;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.language.index.impl.internal.roots.kind.IndexableSetOrigin;
import consulo.language.index.impl.internal.localize.IndexingLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileFilter;

import java.util.List;

/**
 * Scans a known set of files because the options they were indexed with have moved. It exists so that an option
 * provider can ask for those files to be looked at again without marking them changed on disk: the scan consults
 * the recorded options per file and reindexes only the ones that really drifted.
 */
public final class OptionsChangedIndexableFilesIterator implements IndexableFilesIterator {
    private final List<VirtualFile> myFiles;
    private final String myReason;

    public OptionsChangedIndexableFilesIterator(List<VirtualFile> files, String reason) {
        myFiles = List.copyOf(files);
        myReason = reason;
    }

    @Override
    public String getDebugName() {
        return "changed options iterator (" + myReason + ", " + myFiles.size() + " files)";
    }

    @Override
    public LocalizeValue getIndexingProgressText() {
        return IndexingLocalize.indexableFilesProviderIndexingFilesFromPreviousIdeSession();
    }

    @Override
    public IndexableSetOrigin getOrigin() {
        return new OptionsChangedOrigin(myFiles);
    }

    @Override
    public LocalizeValue getRootsScanningProgressText() {
        return LocalizeValue.empty();
    }

    @Override
    public boolean iterateFiles(Project project, ContentIterator fileIterator, VirtualFileFilter fileFilter) {
        return IndexableFilesIterationMethods.iterateRoots(project, myFiles, fileIterator, fileFilter);
    }

    /**
     * Queued scans are merged by origin, so the origin carries the files: two requests for different files must
     * both survive, while a repeated request for the same files may collapse.
     */
    public record OptionsChangedOrigin(List<VirtualFile> files) implements IndexableSetOrigin {
    }
}

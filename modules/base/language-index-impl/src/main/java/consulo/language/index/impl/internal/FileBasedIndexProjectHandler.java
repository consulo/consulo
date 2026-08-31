// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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

import consulo.application.progress.ProgressIndicator;
import consulo.application.util.function.Processor;
import consulo.application.util.registry.Registry;
import consulo.content.ContentIterator;
import consulo.language.index.impl.internal.localize.IndexingLocalize;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.language.index.impl.internal.roots.kind.IndexableSetOrigin;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.DumbModeTask;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileFilter;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author max
 */
public final class FileBasedIndexProjectHandler {
    private static final Logger LOG = Logger.getInstance(FileBasedIndexProjectHandler.class);

    public static final int ourMinFilesToStartDumMode = Registry.intValue("ide.dumb.mode.minFilesToStart", 20);
    private static final int ourMinFilesSizeToStartDumMode = Registry.intValue("ide.dumb.mode.minFilesSizeToStart", 1048576);

    public static void scheduleReindexingInDumbMode(Project project) {
        FileBasedIndex i = FileBasedIndex.getInstance();
        if (i instanceof FileBasedIndexImpl index
            && IndexInfrastructure.hasIndices()
            && !project.isDisposed()
            && mightHaveManyChangedFilesInProject(project, index)) {
            new ProjectChangedFilesScanner(project).queue(project);
        }
    }

    public static boolean mightHaveManyChangedFilesInProject(Project project, FileBasedIndexImpl index) {
        long start = System.currentTimeMillis();
        return !index.processChangedFiles(project, new Processor<>() {
            int filesInProjectToBeIndexed;
            long sizeOfFilesToBeIndexed;

            @Override
            public boolean process(VirtualFile file) {
                ++filesInProjectToBeIndexed;
                if (file.isValid() && !file.isDirectory()) {
                    sizeOfFilesToBeIndexed += file.getLength();
                }
                return filesInProjectToBeIndexed < ourMinFilesToStartDumMode && sizeOfFilesToBeIndexed < ourMinFilesSizeToStartDumMode && System.currentTimeMillis() < start + 100;
            }
        });
    }

    private static class ProjectChangedFilesScanner extends DumbModeTask {
        private final Project myProject;

        private ProjectChangedFilesScanner(Project project) {
            myProject = project;
        }

        @Override
        public void performInDumbMode(ProgressIndicator indicator, Exception trace) {
            indicator.setIndeterminate(false);
            indicator.setText(IndexingLocalize.progressIndexingUpdating());

            Map<IndexableFilesIterator, Collection<VirtualFile>> files = scan();

            if (files.isEmpty()) {
                LOG.info("Finished for " + myProject.getName() + ". No files to index.");
                return;
            }

            // We would like to use UnindexedFilesIndexer.queue (instead of UnindexedFilesIndexer.indexFiles), but this will lead to redundant scanning tasks.
            // Consider the following situation (we use the following notation: [executing] + [queued] + [new tasks] ; comment):
            //
            // [] + [] + [scanning] ; some thread submitted scanning task
            // [] + [scanning] + []
            // [scanning] + [] + []
            // [scanning] + [] + [scanning]  ; some thread submitted scanning task while the other scanning task is in progress. They are not merged in this case
            // [scanning] + [scanning] + []
            // [scanning] + [scanning] + [indexing]    ; first scanning task is about to complete and submitted an indexing task
            // [] + [scanning, indexing] + []          ; first scanning task finished. We denote this state as (A)
            // [scanning] + [indexing] + []
            // [scanning] + [indexing] + [scanning]    ; some thread submitted new scanning task
            // [scanning] + [indexing, scanning] + []
            // [scanning] + [indexing, scanning] + [indexing] ; scanning task submitted an indexing task
            // [scanning] + [scanning, indexing] + []         ; indexing tasks are merged, merged task in inserted at the end of the queue
            // [] + [scanning, indexing] + []                 ; scanning task finished. This is exactly the same state as (A) 5 lines above
            //
            // Fair amount of FileBasedIndexProjectHandler.scheduleReindexingInDumbMode invocations are observed during massive refresh (e.g. branch change).
            // In practice, we observe 2-3 scanning tasks followed by a single indexing task.
            new UnindexedFilesIndexer(myProject, files).indexFiles(indicator);
        }

        private Map<IndexableFilesIterator, Collection<VirtualFile>> scan() {
            long refreshedFilesCalcDuration = System.nanoTime();
            Collection<VirtualFile> files = Collections.emptyList();
            try {
                FileBasedIndexImpl fileBasedIndex = (FileBasedIndexImpl) FileBasedIndex.getInstance();
                files = fileBasedIndex.getFilesToUpdate(myProject);
                IndexableFilesIterator provider = new IndexableFilesIteratorForRefreshedFiles(myProject);
                return Collections.singletonMap(provider, files);
            }
            finally {
                refreshedFilesCalcDuration = System.nanoTime() - refreshedFilesCalcDuration;
                LOG.info("Scanning refreshed files of " + myProject.getName() + " : " + files.size() + " to update, " +
                    "calculated in " + TimeUnit.NANOSECONDS.toMillis(refreshedFilesCalcDuration) + "ms");
            }
        }

        @Override
        public @Nullable DumbModeTask tryMergeWith(DumbModeTask taskFromQueue) {
            if (taskFromQueue instanceof ProjectChangedFilesScanner task && task.myProject.equals(myProject)) {
                return this;
            }
            return null;
        }
    }

    private static class IndexableFilesIteratorForRefreshedFiles implements IndexableFilesIterator {
        private final Project myProject;

        private IndexableFilesIteratorForRefreshedFiles(Project project) {
            myProject = project;
        }

        @Override
        public String getDebugName() {
            return "Refreshed files";
        }

        @Override
        public LocalizeValue getIndexingProgressText() {
            return IndexingLocalize.progressIndexingUpdating();
        }

        @Override
        public LocalizeValue getRootsScanningProgressText() {
            return IndexingLocalize.progressIndexingScanning();
        }

        @Override
        public IndexableSetOrigin getOrigin() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean iterateFiles(Project project, ContentIterator fileIterator, VirtualFileFilter fileFilter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object o) {
            // We need equals because otherwise UnindexedFilesIndexer will not be able to merge files (it merges files per provider, not globally,
            // i.e. the same file assigned to different providers may be indexed twice)
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            IndexableFilesIteratorForRefreshedFiles files = (IndexableFilesIteratorForRefreshedFiles) o;
            // We don't expect that IndexableFilesIterator for different projects be compared, so we could return true here.
            // But it's better to be on safe side, that's why we compare projects.
            return Objects.equals(myProject, files.myProject);
        }

        @Override
        public int hashCode() {
            return myProject.hashCode();
        }
    }
}

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
import consulo.language.index.impl.internal.localize.IndexingLocalize;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.logging.Logger;
import consulo.project.DumbModeTask;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class UnindexedFilesIndexer extends DumbModeTask {
    private static final Logger LOG = Logger.getInstance(UnindexedFilesIndexer.class);

    private final Project myProject;
    private final FileBasedIndexImpl myIndex;
    private final Map<IndexableFilesIterator, Collection<VirtualFile>> myProviderToFiles;

    UnindexedFilesIndexer(Project project, Map<IndexableFilesIterator, Collection<VirtualFile>> providerToFiles) {
        myProject = project;
        myIndex = (FileBasedIndexImpl) FileBasedIndex.getInstance();
        myProviderToFiles = providerToFiles;
    }

    void indexFiles(ProgressIndicator indicator) {
        int totalFiles = myProviderToFiles.values().stream().mapToInt(Collection::size).sum();
        if (totalFiles == 0) {
            LOG.info("Finished for " + myProject.getName() + ". No files to index with loading content.");
            return;
        }

        LOG.info("Unindexed files update started: " + totalFiles + " files to index");

        indicator.setIndeterminate(false);
        indicator.setText(IndexingLocalize.progressIndexingUpdating());

        long startedAt = System.nanoTime();
        for (Map.Entry<IndexableFilesIterator, Collection<VirtualFile>> entry : myProviderToFiles.entrySet()) {
            Collection<VirtualFile> files = entry.getValue();
            if (files.isEmpty()) {
                continue;
            }
            indicator.setText(entry.getKey().getIndexingProgressText());
            CacheUpdateRunner.processFiles(indicator, List.copyOf(files), myProject, content -> myIndex.indexFileContent(myProject, content));
        }
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        LOG.info("Unindexed files update finished: " + totalFiles + " files in " + elapsedMillis + " ms on "
            + CacheUpdateRunner.indexingThreadCount() + " threads ("
            + (elapsedMillis == 0 ? totalFiles : totalFiles * 1000L / elapsedMillis) + " files/s)");
    }

    @Override
    public void performInDumbMode(ProgressIndicator indicator, Exception trace) {
        indexFiles(indicator);
    }

    @Override
    public @Nullable DumbModeTask tryMergeWith(DumbModeTask taskFromQueue) {
        if (taskFromQueue.getClass() != getClass()) {
            return null;
        }
        UnindexedFilesIndexer otherIndexingTask = (UnindexedFilesIndexer) taskFromQueue;
        if (!otherIndexingTask.myProject.equals(myProject)) {
            return null;
        }

        Map<IndexableFilesIterator, Collection<VirtualFile>> mergedFiles = new HashMap<>();
        for (Map.Entry<IndexableFilesIterator, Collection<VirtualFile>> entry : otherIndexingTask.myProviderToFiles.entrySet()) {
            mergedFiles.computeIfAbsent(entry.getKey(), k -> new java.util.ArrayList<>()).addAll(entry.getValue());
        }
        for (Map.Entry<IndexableFilesIterator, Collection<VirtualFile>> entry : myProviderToFiles.entrySet()) {
            mergedFiles.computeIfAbsent(entry.getKey(), k -> new java.util.ArrayList<>()).addAll(entry.getValue());
        }
        return new UnindexedFilesIndexer(myProject, mergedFiles);
    }

    @Override
    public String toString() {
        return "UnindexedFilesIndexer[" + myProject.getName() + ", " + myProviderToFiles.size() + " providers]";
    }
}

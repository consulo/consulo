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
package consulo.language.index.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.progress.ProgressIndicator;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Collects files found by the scanner ({@link UnindexedFilesScanner}) to be indexed later in one batch.
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class PerProjectIndexingQueue {
    /** Not thread safe */
    public interface PerProviderSink {
        void addFile(VirtualFile file);

        void commit();
    }

    private static final Logger LOG = Logger.getInstance(PerProjectIndexingQueue.class);

    public static PerProjectIndexingQueue getInstance(Project project) {
        return project.getInstance(PerProjectIndexingQueue.class);
    }

    private final Project myProject;

    // guarded by [myLock]. Must be in consistent state under write lock (see [myLock] comment)
    // Total count of VirtualFile in myFilesSoFar. This is (arguable) performance optimization
    private final AtomicInteger myCntFilesSoFar = new AtomicInteger();

    // guarded by [myLock]. Must be in consistent state under write lock (see [myLock] comment)
    // Files that will be re-indexed
    private volatile ConcurrentMap<IndexableFilesIterator, Collection<VirtualFile>> myFilesSoFar = new ConcurrentHashMap<>();

    // Code under read lock still runs in parallel, so all the counters and collections still have
    // to be thread-safe. It is only required that the state must be consistent under write lock (e.g. myCntFilesSoFar corresponds to total
    // count of files in myFilesSoFar)
    private final ReentrantReadWriteLock myLock = new ReentrantReadWriteLock();

    @Inject
    public PerProjectIndexingQueue(Project project) {
        myProject = project;
    }

    // `private` because we want clients to use [PerProviderSink] which forces them to report files one by one.
    private void addFiles(IndexableFilesIterator iterator, List<VirtualFile> files) {
        myLock.readLock().lock();
        try {
            myFilesSoFar.compute(iterator, (i, old) -> {
                if (old == null) {
                    return new ArrayList<>(files);
                }
                Collection<VirtualFile> merged = new ArrayList<>(old);
                merged.addAll(files);
                return merged;
            });
            myCntFilesSoFar.addAndGet(files.size());
        }
        finally {
            myLock.readLock().unlock();
        }
    }

    public void flushNow() {
        Pair<ConcurrentMap<IndexableFilesIterator, Collection<VirtualFile>>, Integer> queued = getAndResetQueuedFiles();
        if (queued.getSecond() > 0) {
            new UnindexedFilesIndexer(myProject, queued.getFirst()).queue(myProject);
        }
        else {
            LOG.info("Finished for " + myProject.getName() + ". No files to index with loading content.");
        }
    }

    public void flushNowSync(ProgressIndicator indicator) {
        Pair<ConcurrentMap<IndexableFilesIterator, Collection<VirtualFile>>, Integer> queued = getAndResetQueuedFiles();
        if (queued.getSecond() > 0) {
            new UnindexedFilesIndexer(myProject, queued.getFirst()).indexFiles(indicator);
        }
        else {
            LOG.info("Finished for " + myProject.getName() + ". No files to index with loading content.");
        }
    }

    private Pair<ConcurrentMap<IndexableFilesIterator, Collection<VirtualFile>>, Integer> getAndResetQueuedFiles() {
        myLock.writeLock().lock();
        try {
            ConcurrentMap<IndexableFilesIterator, Collection<VirtualFile>> filesInQueue = myFilesSoFar;
            myFilesSoFar = new ConcurrentHashMap<>();
            int totalFiles = myCntFilesSoFar.getAndSet(0);
            return Pair.create(filesInQueue, totalFiles);
        }
        finally {
            myLock.writeLock().unlock();
        }
    }

    public PerProviderSink getSink(IndexableFilesIterator provider) {
        return new PerProviderSinkImpl(provider);
    }

    private class PerProviderSinkImpl implements PerProviderSink {
        private final IndexableFilesIterator myIterator;
        private final List<VirtualFile> myFiles = new ArrayList<>();
        private boolean myCommitted;

        PerProviderSinkImpl(IndexableFilesIterator iterator) {
            myIterator = iterator;
        }

        @Override
        public void addFile(VirtualFile file) {
            LOG.assertTrue(!myCommitted, "Should not invoke 'addFile' after 'commit'");
            myFiles.add(file);
        }

        @Override
        public void commit() {
            myCommitted = true;
            if (!myFiles.isEmpty()) {
                addFiles(myIterator, myFiles);
            }
        }
    }
}

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

import consulo.application.ApplicationManager;
import consulo.application.util.registry.Registry;
import consulo.project.Project;
import consulo.project.internal.UnindexedFilesScannerExecutor;

import static consulo.util.lang.SystemProperties.getBooleanProperty;

public final class UnindexedFilesUpdater {
    private static final boolean USE_CONSERVATIVE_THREAD_COUNT_POLICY =
        getBooleanProperty("idea.indexing.use.conservative.thread.count.policy", false);

    private static final int DEFAULT_MAX_INDEXER_THREADS = 4;
    /**
     * Defines number of indexing threads. -1 means autoconfigured value
     * (see getNumberOfIndexingThreads/getMaxNumberOfIndexingThreads for algo).
     */
    private static final int INDEXER_THREAD_COUNT = Registry.intValue("caches.indexerThreadsCount", -1);
    /**
     * Count CPU# with or without hyper-threading:
     * if true:  assume # cores reported is # physical cores x2, so /2 to get physical cores count
     * If false (default): use # cores reported as-is, don't try to outsmart CPU developers
     */
    private static final boolean IS_HT_SMT_ENABLED = getBooleanProperty("intellij.system.ht.smt.enabled", false);

    private UnindexedFilesUpdater() {
    }

    /**
     * Returns the best number of threads to be used for indexing at this moment.
     * It may change during execution of the IDE depending on other activities' load.
     */
    public static int getNumberOfIndexingThreads() {
        int threadCount = INDEXER_THREAD_COUNT;
        if (threadCount <= 0) {
            threadCount = Math.max(1, Math.min(USE_CONSERVATIVE_THREAD_COUNT_POLICY
                ? DEFAULT_MAX_INDEXER_THREADS : getMaxNumberOfIndexingThreads(), getMaxNumberOfIndexingThreads()));
        }
        return threadCount;
    }

    /**
     * Returns the maximum number of threads to be used for indexing during this execution of the IDE.
     */
    public static int getMaxNumberOfIndexingThreads() {
        // Change of the registry option requires IDE restart.
        int threadCount = INDEXER_THREAD_COUNT;
        if (threadCount > 0) {
            return threadCount;
        }

        return Math.max(1, getAvailablePhysicalCoresNumber() - getCoresToLeaveForOtherActivitiesCount());
    }

    public static int getAvailablePhysicalCoresNumber() {
        int availableCores = Runtime.getRuntime().availableProcessors();
        return IS_HT_SMT_ENABLED ? availableCores / 2 : availableCores;
    }

    /**
     * Scanning activity can be scaled well across number of threads, so we're trying to use all available resources to do it faster.
     */
    public static int getNumberOfScanningThreads() {
        int scanningThreadCount = Registry.intValue("caches.scanningThreadsCount");
        if (scanningThreadCount > 0) {
            return scanningThreadCount;
        }
        int maxBackgroundThreadCount = getMaxBackgroundThreadCount();
        return Math.max(maxBackgroundThreadCount, getNumberOfIndexingThreads());
    }

    private static int getMaxBackgroundThreadCount() {
        // note that getMaxBackgroundThreadCount is used to calculate threads count is FilesScanExecutor,
        // which is also used for "FindInFiles"
        return Runtime.getRuntime().availableProcessors() - getCoresToLeaveForOtherActivitiesCount();
    }

    private static int getCoresToLeaveForOtherActivitiesCount() {
        return ApplicationManager.getApplication().isCommandLine() ? 0 : 1;
    }

    public static boolean isScanningInProgress(Project project) {
        UnindexedFilesScannerExecutor executor = UnindexedFilesScannerExecutor.getInstance(project);
        return executor.hasQueuedTasks() || executor.isRunning().get();
    }
}

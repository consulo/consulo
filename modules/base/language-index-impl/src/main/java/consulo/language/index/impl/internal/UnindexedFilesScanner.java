// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal;

import consulo.application.ApplicationManager;
import consulo.application.impl.internal.performance.PerformanceWatcher;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.component.ProcessCanceledException;
import consulo.content.ContentIterator;
import consulo.language.index.impl.internal.localize.IndexingLocalize;
import consulo.language.index.impl.internal.roots.IndexableFilesDeduplicateFilter;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.language.index.impl.internal.roots.kind.SdkOrigin;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.logging.Logger;
import consulo.module.content.PushedFilePropertiesUpdater;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.project.DumbModeTask;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Eugene Zhuravlev
 */
public class UnindexedFilesScanner extends DumbModeTask {
    private static final Logger LOG = Logger.getInstance(UnindexedFilesScanner.class);

    private static final Key<Boolean> CONTENT_SCANNED = Key.create("CONTENT_SCANNED");
    private static final Key<Boolean> INDEX_UPDATE_IN_PROGRESS = Key.create("INDEX_UPDATE_IN_PROGRESS");

    private final FileBasedIndexImpl myIndex;
    private final Project myProject;
    private final PushedFilePropertiesUpdater myPusher;

    public UnindexedFilesScanner(final Project project) {
        myProject = project;
        myPusher = PushedFilePropertiesUpdater.getInstance(myProject);
        myIndex = (FileBasedIndexImpl) FileBasedIndex.getInstance();

        myProject.putUserData(CONTENT_SCANNED, null);

        project.getMessageBus().connect(this).subscribe(ModuleRootListener.class, new ModuleRootListener() {
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                DumbService.getInstance(project).cancelTask(UnindexedFilesScanner.this);
            }
        });
    }

    public static boolean isIndexUpdateInProgress(Project project) {
        return project.getUserData(INDEX_UPDATE_IN_PROGRESS) == Boolean.TRUE;
    }

    public static boolean isProjectContentFullyScanned(Project project) {
        return Boolean.TRUE.equals(project.getUserData(CONTENT_SCANNED));
    }

    private void updateUnindexedFiles(ProgressIndicator indicator, Exception trace) {
        if (!IndexInfrastructure.hasIndices()) {
            return;
        }

        PerformanceWatcher.Snapshot snapshot = PerformanceWatcher.takeSnapshot();
        myPusher.pushAllPropertiesNow();
        boolean trackResponsiveness = !ApplicationManager.getApplication().isUnitTestMode();

        if (trackResponsiveness) {
            snapshot.logResponsivenessSinceCreation("Pushing properties");
        }

        indicator.setIndeterminate(true);
        indicator.setText(IndexingLocalize.progressIndexingScanning());

        myIndex.clearIndicesIfNecessary();

        List<IndexableFilesIterator> orderedProviders = getOrderedProviders();

        snapshot = PerformanceWatcher.takeSnapshot();

        collectIndexableFilesConcurrently(myProject, indicator, orderedProviders, trace);

        if (trackResponsiveness) {
            snapshot.logResponsivenessSinceCreation("Indexable file iteration");
        }

        myProject.putUserData(CONTENT_SCANNED, true);

        // the full VFS refresh makes sense only after it's loaded, i.e., after scanning files to index is finished
        InitialVfsRefreshService.getInstance(myProject).scheduleInitialVfsRefresh();

        PerProjectIndexingQueue.getInstance(myProject).flushNowSync(indicator);
    }

    /**
     * Returns providers of files. The order of the providers is not strictly specified.
     * This method moves all SDK providers to the end.
     */
    private List<IndexableFilesIterator> getOrderedProviders() {
        List<IndexableFilesIterator> originalOrderedProviders = myIndex.getOrderedIndexableFilesProviders(myProject);

        List<IndexableFilesIterator> orderedProviders = new ArrayList<>();
        originalOrderedProviders.stream()
            .filter(p -> !(p.getOrigin() instanceof SdkOrigin))
            .collect(Collectors.toCollection(() -> orderedProviders));

        originalOrderedProviders.stream()
            .filter(p -> p.getOrigin() instanceof SdkOrigin)
            .collect(Collectors.toCollection(() -> orderedProviders));

        return orderedProviders;
    }

    private void collectIndexableFilesConcurrently(
        Project project,
        ProgressIndicator indicator,
        List<IndexableFilesIterator> providers,
        Exception trace
    ) {
        if (providers.isEmpty()) {
            return;
        }

        PerProjectIndexingQueue indexingQueue = PerProjectIndexingQueue.getInstance(project);
        IndexableFilesDeduplicateFilter indexableFilesDeduplicateFilter = IndexableFilesDeduplicateFilter.create();

        indicator.setText(IndexingLocalize.progressIndexingScanning());
        indicator.setIndeterminate(true);

        List<Runnable> tasks = ContainerUtil.map(providers, provider -> {
            IndexableFilesDeduplicateFilter thisProviderDeduplicateFilter =
                IndexableFilesDeduplicateFilter.createDelegatingTo(indexableFilesDeduplicateFilter);

            return () -> {
                indicator.setText(provider.getRootsScanningProgressText());

                List<VirtualFile> files = new ArrayList<>();
                ContentIterator collectingIterator = myIndex.createUnindexedFilesFinder(files);
                provider.iterateFiles(project, fileOrDir -> {
                    ProgressManager.checkCanceled(); // give a chance to suspend indexing
                    return collectingIterator.processFile(fileOrDir);
                }, thisProviderDeduplicateFilter);

                PerProjectIndexingQueue.PerProviderSink sink = indexingQueue.getSink(provider);
                for (VirtualFile file : files) {
                    sink.addFile(file);
                }
                sink.commit();
            };
        });

        PushedFilePropertiesUpdaterImpl.invokeConcurrentlyIfPossible(trace, ApplicationManager.getApplication(), tasks);
    }

    @Override
    public @Nullable DumbModeTask tryMergeWith(DumbModeTask taskFromQueue) {
        if (taskFromQueue.getClass() == getClass() && myProject.equals(((UnindexedFilesScanner) taskFromQueue).myProject)) {
            return this;
        }
        return null;
    }

    @Override
    public void performInDumbMode(ProgressIndicator indicator, Exception trace) {
        myProject.putUserData(INDEX_UPDATE_IN_PROGRESS, true);
        try {
            myIndex.filesUpdateStarted(myProject);
            try {
                updateUnindexedFiles(indicator, trace);
            }
            catch (ProcessCanceledException e) {
                LOG.info("Unindexed files update canceled");
                throw e;
            }
            finally {
                myIndex.filesUpdateFinished(myProject);
            }
        }
        finally {
            myProject.putUserData(INDEX_UPDATE_IN_PROGRESS, false);
        }
    }
}

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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.content.bundle.Sdk;
import consulo.content.library.Library;
import consulo.disposer.Disposer;
import consulo.language.index.impl.internal.BuildableRootsChangeRescanningInfoImpl.BuiltRescanningInfo;
import consulo.language.index.impl.internal.roots.CustomOrderEntryIndexableFilesIterator;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.language.index.impl.internal.roots.LibraryIndexableFilesIteratorImpl;
import consulo.language.index.impl.internal.roots.ModuleIndexableFilesIteratorImpl;
import consulo.language.index.impl.internal.roots.SdkIndexableFilesIteratorImpl;
import consulo.language.index.impl.internal.roots.kind.IndexableSetOrigin;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.internal.BuildableRootsChangeRescanningInfo;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.internal.EntityIndexingService;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntryWithTracking;
import consulo.project.Project;
import consulo.project.RootsChangeRescanningInfo;
import consulo.project.impl.internal.DumbServiceImpl;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Singleton
@ServiceImpl
public final class EntityIndexingServiceImpl implements EntityIndexingService {
    private static final Logger LOG = Logger.getInstance(EntityIndexingServiceImpl.class);
    private static final RootChangesLogger ROOT_CHANGES_LOGGER = new RootChangesLogger();

    private final Application myApplication;

    @Inject
    public EntityIndexingServiceImpl(Application application) {
        myApplication = application;
    }

    @Override
    public void indexChanges(Project project, List<? extends RootsChangeRescanningInfo> changes) {
        if (!(FileBasedIndex.getInstance() instanceof FileBasedIndexImpl)) {
            return;
        }
        if (UnindexedFilesScannerStartup.invalidateProjectFilterIfFirstScanningNotRequested(project)) {
            return;
        }

        Throwable trace = new Throwable();
        if (changes.isEmpty()) {
            logRootChanges(project, true, trace);
            new UnindexedFilesScanner(project, "Project roots have changed").queue();
        }
        else {
            CompletableFuture<ScanningParameters> parameters = computeScanningParameters(project, changes, trace);
            new UnindexedFilesScanner(project, parameters).queue();
        }
    }

    private CompletableFuture<ScanningParameters> computeScanningParameters(
        Project project,
        List<? extends RootsChangeRescanningInfo> changes,
        Throwable trace
    ) {
        if (DumbServiceImpl.isSynchronousTaskExecution()) {
            return CompletableFuture.completedFuture(ReadAction.compute(() -> doComputeScanningParameters(project, changes, trace)));
        }

        CoroutineScope scope = CoroutineScope.of(project.coroutineContext());
        return Coroutine.first(ReadLock.<Object, ScanningParameters>apply(ignored -> doComputeScanningParameters(project, changes, trace)))
            .runAsync(scope, null)
            .toFuture();
    }

    @RequiredReadAction
    private ScanningParameters doComputeScanningParameters(
        Project project,
        List<? extends RootsChangeRescanningInfo> changes,
        Throwable trace
    ) {
        for (RootsChangeRescanningInfo change : changes) {
            if (change == RootsChangeRescanningInfo.TOTAL_RESCAN) {
                return fullRescan(project, "Reindex requested by project root model changes", trace);
            }
            else if (change == RootsChangeRescanningInfo.RESCAN_DEPENDENCIES_IF_NEEDED) {
                return fullRescan(project, "Reindex of changed dependencies requested, but not enabled", trace);
            }
        }

        List<IndexableFilesIterator> iterators = new SmartList<>();
        for (RootsChangeRescanningInfo change : changes) {
            if (change == RootsChangeRescanningInfo.NO_RESCAN_NEEDED || change == RootsChangeRescanningInfo.RESCAN_DEPENDENCIES_IF_NEEDED) {
                continue;
            }
            if (change instanceof BuiltRescanningInfo builtInfo) {
                iterators.addAll(getIteratorsOnBuildableChangeInfo(builtInfo, project));
            }
            else {
                LOG.warn("Unexpected change " + change.getClass() + " " + change + ", full reindex requested");
                return fullRescan(project, "Reindex on unexpected change in EntityIndexingServiceImpl", trace);
            }
        }

        if (!iterators.isEmpty()) {
            List<String> debugNames = ContainerUtil.map(iterators, IndexableFilesIterator::getDebugName);
            LOG.debug("Accumulated iterators: " + debugNames);
            int maxNamesToLog = 10;
            String reasonMessage = "changes in: " + debugNames
                .stream()
                .limit(maxNamesToLog)
                .map(StringUtil::wrapWithDoubleQuote).collect(Collectors.joining(", "));
            if (debugNames.size() > maxNamesToLog) {
                reasonMessage += " and " + (debugNames.size() - maxNamesToLog) + " iterators more";
            }
            logRootChanges(project, false, trace);
            return new ScanningIterators(reasonMessage, iterators);
        }
        return CancelledScanning.INSTANCE;
    }

    private ScanningParameters fullRescan(Project project, String reason, Throwable trace) {
        logRootChanges(project, true, trace);
        return new ScanningIterators(reason);
    }

    private void logRootChanges(Project project, boolean isFullReindex, Throwable trace) {
        if (myApplication.isUnitTestMode()) {
            if (LOG.isDebugEnabled()) {
                String message = isFullReindex ?
                    "Project roots of " + project.getName() + " have changed" :
                    "Project roots of " + project.getName() + " will be partially reindexed";
                LOG.debug(message, trace);
            }
        }
        else {
            ROOT_CHANGES_LOGGER.info(project, isFullReindex, trace);
        }
    }

    @RequiredReadAction
    private static List<IndexableFilesIterator> getIteratorsOnBuildableChangeInfo(BuiltRescanningInfo info, Project project) {
        Map<IndexableSetOrigin, IndexableFilesIterator> iterators = new LinkedHashMap<>();
        for (Module module : info.modules()) {
            if (module.isDisposed()) {
                continue;
            }
            for (IndexableFilesIterator moduleIterator : ModuleIndexableFilesIteratorImpl.getModuleIterators(module)) {
                addIterator(iterators, moduleIterator);
            }
            for (OrderEntry orderEntry : ModuleRootManager.getInstance(module).getOrderEntries()) {
                addOrderEntryIterator(iterators, orderEntry);
            }
        }
        if (info.hasInheritedSdk()) {
            LOG.debug("Inherited sdk change is ignored for " + project.getName() + ": there is no project-level inherited sdk");
        }
        for (Sdk sdk : info.sdks()) {
            addIterators(iterators, SdkIndexableFilesIteratorImpl.createIterators(sdk));
        }
        for (Library library : info.libraries()) {
            if (Disposer.isDisposed(library)) {
                continue;
            }
            addIterators(iterators, LibraryIndexableFilesIteratorImpl.createIterators(library));
        }
        for (OrderEntry orderEntry : info.orderEntries()) {
            addOrderEntryIterator(iterators, orderEntry);
        }
        return new ArrayList<>(iterators.values());
    }

    @RequiredReadAction
    private static void addOrderEntryIterator(Map<IndexableSetOrigin, IndexableFilesIterator> iterators, OrderEntry orderEntry) {
        if (!orderEntry.isValid()) {
            return;
        }
        if (orderEntry instanceof LibraryOrderEntry libraryOrderEntry) {
            Library library = libraryOrderEntry.getLibrary();
            if (library != null && !Disposer.isDisposed(library)) {
                addIterators(iterators, LibraryIndexableFilesIteratorImpl.createIterators(library));
            }
        }
        else if (orderEntry instanceof ModuleExtensionWithSdkOrderEntry sdkOrderEntry) {
            Sdk sdk = sdkOrderEntry.getSdk();
            if (sdk != null) {
                addIterators(iterators, SdkIndexableFilesIteratorImpl.createIterators(sdk));
            }
        }
        else if (orderEntry instanceof OrderEntryWithTracking) {
            addIterator(iterators, CustomOrderEntryIndexableFilesIterator.createIterator(orderEntry));
        }
    }

    private static void addIterators(
        Map<IndexableSetOrigin, IndexableFilesIterator> iterators,
        Collection<? extends IndexableFilesIterator> newIterators
    ) {
        for (IndexableFilesIterator iterator : newIterators) {
            addIterator(iterators, iterator);
        }
    }

    private static void addIterator(Map<IndexableSetOrigin, IndexableFilesIterator> iterators, IndexableFilesIterator iterator) {
        iterators.putIfAbsent(iterator.getOrigin(), iterator);
    }

    @Override
    public BuildableRootsChangeRescanningInfo createBuildableInfoBuilder() {
        return new BuildableRootsChangeRescanningInfoImpl();
    }
}

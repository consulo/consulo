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
package consulo.language.index.impl.internal.moduleAware;

import consulo.index.io.ID;
import consulo.language.index.impl.internal.IndexingStamp;
import consulo.application.AccessRule;
import consulo.language.psi.stub.ModuleAwareIndexOptions;
import consulo.document.util.FileContentUtilCore;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.psi.stub.IndexedFile;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.ModuleAwareIndexOptionProvider;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.util.collection.primitive.ints.ConcurrentIntObjectMap;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Write-path hook: called by {@code FileBasedIndexImpl} after a successful
 * {@code updateSingleIndex(indexId, file, ...)} to record the current options
 * {@link OptionsMeta} for the {@code (indexId, file)} pair.
 *
 * <p>The listener side ({@code ModuleAwareIndexRootChangeListener}) reads back this meta
 * on {@code rootsChanged} to decide whether a reindex is needed. Together these two sides
 * give end-to-end options-drift detection.</p>
 *
 * <p>Read-path cost control: the per-provider state (options payload serialized + hashed)
 * is cached per {@code (project, fileId)}, invalidated by the roots modification count.
 * The write path always computes fresh state — non-roots inputs (e.g. a pre-indexing pass'
 * entry states) may change without a roots increment, and the meta recorded after a
 * reindex must reflect what the indexer actually consumed.</p>
 *
 * <p>Missing meta on an already-indexed file counts as stale: the file was indexed before
 * its provider existed (e.g. the plugin was installed later), so the stored index data was
 * produced under unknown options.</p>
 */
public final class ModuleAwareIndexMetaRecorder {
    private record ProviderStateCache(long rootsStamp, ConcurrentIntObjectMap<List<Map<String, OptionsMeta.PerProviderMeta>>> byFile) {
    }

    private static final ExtensionPointCacheKey<FileBasedIndexExtension, Map<ID<?, ?>, FileBasedIndexExtension<?, ?>>> BY_INDEX_ID =
        ExtensionPointCacheKey.create("ModuleAwareIndexMetaRecorder.byIndexId", walker -> {
            Map<ID<?, ?>, FileBasedIndexExtension<?, ?>> byId = new HashMap<>();
            walker.walk(extension -> byId.put(extension.getName(), extension));
            return byId;
        });
    private static final ExtensionPointCacheKey<FileBasedIndexExtension, List<FileBasedIndexExtension<?, ?>>> OPTIONS_SENSITIVE =
        ExtensionPointCacheKey.create("ModuleAwareIndexMetaRecorder.optionsSensitive", walker -> {
            List<FileBasedIndexExtension<?, ?>> sensitive = new ArrayList<>();
            walker.walk(extension -> {
                if (!extension.getOptionProviderIds().isEmpty()) {
                    sensitive.add(extension);
                }
            });
            return List.copyOf(sensitive);
        });
    private static final Key<AtomicReference<ProviderStateCache>> PROVIDER_STATE_CACHE = Key.create("module.aware.provider.state.cache");
    private static final Set<Integer> ourReindexRequested = ConcurrentHashMap.newKeySet();
    private static final Set<VirtualFile> ourPendingReparse = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean ourReparseScheduled = new AtomicBoolean();

    private ModuleAwareIndexMetaRecorder() {
    }

    /**
     * Read-path hook: returns {@code true} if stored {@link OptionsMeta} for
     * {@code (indexId, file)} no longer matches the current provider state, meaning the
     * cached index entry is stale for this module's options. Callers should drop the
     * cached result and trigger a rescan via {@link #requestRescanOnce}.
     */
    public static boolean isStale(ID<?, ?> indexId,
                                  VirtualFile file,
                                  @Nullable Project project) {
        if (project == null || !(file instanceof VirtualFileWithId withId)) {
            return false;
        }

        FileBasedIndexExtension<?, ?> extension = findExtension(indexId);
        if (extension == null) {
            return false;
        }
        List<String> requestedIds = extension.getOptionProviderIds();
        if (requestedIds.isEmpty()) {
            return false;
        }

        int fileId = withId.getId();

        Module module = ProjectFileIndex.getInstance(project).getModuleForFile(file);
        if (module == null) {
            ourReindexRequested.remove(fileId);
            return false;
        }

        List<ModuleAwareIndexOptionProvider> applicable = applicableOf(requestedIds, file.getFileType());
        if (applicable.isEmpty()) {
            ourReindexRequested.remove(fileId);
            return false;
        }

        OptionsMeta stored = ModuleAwareIndexMetaStorage.getInstance().get(indexId, fileId);
        if (stored == null) {
            if (IndexingStamp.isFileIndexedStateCurrent(fileId, indexId).isUpToDate()) {
                return true;
            }
            ourReindexRequested.remove(fileId);
            return false;
        }

        List<Map<String, OptionsMeta.PerProviderMeta>> currentState = cachedState(project, fileId, file, module);
        boolean stale = OptionsRevalidator.needsReindex(extension.getVersion(), stored, idsOf(applicable), currentState);
        if (!stale) {
            ourReindexRequested.remove(fileId);
        }
        return stale;
    }

    /**
     * Asks for the file to be scanned again at most once per staleness episode. The scan consults
     * {@link #isOptionsDrifted} and reindexes the file; marking it changed on disk instead would put it in the
     * dirty-file queue, which is persisted and decides whether the next open may skip its full scan. The guard
     * clears when fresh meta lands ({@link #recordIfApplicable}) or when the file is observed clean again
     * ({@link #isStale} returning {@code false}).
     */
    public static boolean requestRescanOnce(Project project, VirtualFile file) {
        if (!(file instanceof VirtualFileWithId withId)) {
            return false;
        }
        if (ourReindexRequested.add(withId.getId())) {
            ModuleAwareIndexOptions.optionsChanged(project, List.of(file), "index options drifted");
            return true;
        }
        return false;
    }

    /**
     * Options steer the parse as well as the index, so a file whose recorded options moved needs its tree rebuilt
     * once its index has been. Files are batched into one write action per burst of recordings.
     */
    private static void scheduleReparse(VirtualFile file) {
        ourPendingReparse.add(file);
        if (!ourReparseScheduled.compareAndSet(false, true)) {
            return;
        }
        Application application = Application.get();
        application.invokeLater(() -> {
            ourReparseScheduled.set(false);
            List<VirtualFile> files = new ArrayList<>(ourPendingReparse);
            ourPendingReparse.removeAll(files);
            files.removeIf(each -> !each.isValid());
            if (!files.isEmpty()) {
                application.runWriteAction(() -> FileContentUtilCore.reparseFiles(files));
            }
        });
    }

    /**
     * Scanning hook: {@code true} when the options that produced this file's index data no longer match the
     * current ones. The scanner consults it before the persistent indexing flag decides a file needs no work,
     * so a file whose module options drifted is reindexed by the ordinary scan instead of by a separate walk
     * of the whole project.
     */
    public static boolean isOptionsDrifted(IndexedFile indexedFile) {
        List<FileBasedIndexExtension<?, ?>> sensitive = optionsSensitiveExtensions();
        if (sensitive.isEmpty()) {
            return false;
        }

        VirtualFile file = indexedFile.getFile();
        Project project = indexedFile.getProject();
        if (project == null || !(file instanceof VirtualFileWithId)) {
            return false;
        }

        return AccessRule.read(() -> {
            if (project.isDisposed() || !file.isValid()) {
                return Boolean.FALSE;
            }
            for (FileBasedIndexExtension<?, ?> extension : sensitive) {
                if (isStale(extension.getName(), file, project)) {
                    return Boolean.TRUE;
                }
            }
            return Boolean.FALSE;
        });
    }

    /**
     * The extensions that declare option providers. The extension list is fixed for the life of the
     * application, so the answer is computed once: the common case is no provider at all, and then every
     * scanning-side call above costs one empty-list check.
     */
    private static List<FileBasedIndexExtension<?, ?>> optionsSensitiveExtensions() {
        return Application.get().getExtensionPoint(FileBasedIndexExtension.class).getOrBuildCache(OPTIONS_SENSITIVE);
    }

    public static void recordIfApplicable(ID<?, ?> indexId,
                                          VirtualFile file,
                                          @Nullable Project project) {
        if (project == null || !(file instanceof VirtualFileWithId withId)) {
            return;
        }

        FileBasedIndexExtension<?, ?> extension = findExtension(indexId);
        if (extension == null) {
            return;
        }

        List<String> requestedIds = extension.getOptionProviderIds();
        if (requestedIds.isEmpty()) {
            return;
        }

        Module module = ProjectFileIndex.getInstance(project).getModuleForFile(file);
        if (module == null) {
            return;
        }

        List<ModuleAwareIndexOptionProvider> applicable = applicableOf(requestedIds, file.getFileType());
        if (applicable.isEmpty()) {
            return;
        }

        int fileId = withId.getId();
        List<ModuleAwareIndexOptionProvider> allApplicable = ModuleAwareIndexOptionRegistry.getApplicableProviders(file.getFileType());
        List<Map<String, OptionsMeta.PerProviderMeta>> freshState = OptionsRevalidator.currentState(allApplicable, module, file, fileId);

        OptionsMeta snapshot = OptionsRevalidator.snapshot(extension.getVersion(), idsOf(applicable), freshState);
        ModuleAwareIndexMetaStorage storage = ModuleAwareIndexMetaStorage.getInstance();
        OptionsMeta previous = storage.get(indexId, fileId);
        storage.put(indexId, fileId, snapshot);
        if (previous != null && !previous.equals(snapshot)) {
            scheduleReparse(file);
        }

        cacheHolder(project).byFile().put(fileId, freshState);
        ourReindexRequested.remove(fileId);
        dropStaleViewOptions(project, file);
    }

    /**
     * View options survive only while a stored variant carries them; after a reindex that removed the variant the file
     * falls back to its primary one and is reparsed so its tree follows.
     */
    private static void dropStaleViewOptions(Project project, VirtualFile file) {
        Map<String, byte[]> view = ModuleAwareIndexOptions.getViewOptions(file);
        if (view == null) {
            return;
        }
        for (VariantDescriptor descriptor : ModuleAwareIndexVariants.descriptorsFor(project, file)) {
            if (descriptor.matches(view)) {
                return;
            }
        }
        file.putUserData(ModuleAwareIndexOptions.VIEW_OPTIONS, null);
        scheduleReparse(file);
    }

    /**
     * The per-file provider state is cached until the roots change, which is the only invalidation the read path
     * can see on its own. A provider whose options moved for other reasons (an include seed, say) says so through
     * {@link ModuleAwareIndexOptions#optionsChanged}, and that drops the cached state here first, so the scan that
     * follows compares the recorded meta against fresh options rather than against the cache.
     */
    public static void dropCachedState(Project project, Collection<? extends VirtualFile> files) {
        AtomicReference<ProviderStateCache> reference = project.getUserData(PROVIDER_STATE_CACHE);
        ProviderStateCache holder = reference == null ? null : reference.get();
        if (holder == null) {
            return;
        }
        for (VirtualFile file : files) {
            if (file instanceof VirtualFileWithId withId) {
                holder.byFile().remove(withId.getId());
            }
        }
    }

    private static List<Map<String, OptionsMeta.PerProviderMeta>> cachedState(Project project, int fileId, VirtualFile file, Module module) {
        ProviderStateCache holder = cacheHolder(project);
        List<Map<String, OptionsMeta.PerProviderMeta>> state = holder.byFile().get(fileId);
        if (state != null) {
            return state;
        }
        List<ModuleAwareIndexOptionProvider> allApplicable = ModuleAwareIndexOptionRegistry.getApplicableProviders(file.getFileType());
        state = OptionsRevalidator.currentState(allApplicable, module, file, fileId);
        List<Map<String, OptionsMeta.PerProviderMeta>> existing = holder.byFile().putIfAbsent(fileId, state);
        return existing != null ? existing : state;
    }

    private static ProviderStateCache cacheHolder(Project project) {
        long rootsStamp = ProjectRootManager.getInstance(project).getModificationCount();
        AtomicReference<ProviderStateCache> reference = project.getUserData(PROVIDER_STATE_CACHE);
        if (reference == null) {
            reference = project.putUserDataIfAbsent(PROVIDER_STATE_CACHE, new AtomicReference<>());
        }
        while (true) {
            ProviderStateCache holder = reference.get();
            if (holder != null && holder.rootsStamp() == rootsStamp) {
                return holder;
            }
            ProviderStateCache fresh = new ProviderStateCache(rootsStamp, IntMaps.newConcurrentIntObjectHashMap());
            if (reference.compareAndSet(holder, fresh)) {
                return fresh;
            }
        }
    }

    private static List<ModuleAwareIndexOptionProvider> applicableOf(List<String> requestedIds, FileType fileType) {
        Set<String> requested = new HashSet<>(requestedIds);
        List<ModuleAwareIndexOptionProvider> applicable = new ArrayList<>();
        for (ModuleAwareIndexOptionProvider provider : ModuleAwareIndexOptionRegistry.getApplicableProviders(fileType)) {
            if (requested.contains(provider.getId())) {
                applicable.add(provider);
            }
        }
        return applicable;
    }

    private static Set<String> idsOf(List<ModuleAwareIndexOptionProvider> providers) {
        Set<String> ids = new HashSet<>(providers.size());
        for (ModuleAwareIndexOptionProvider provider : providers) {
            ids.add(provider.getId());
        }
        return ids;
    }

    private static @Nullable FileBasedIndexExtension<?, ?> findExtension(ID<?, ?> indexId) {
        return Application.get().getExtensionPoint(FileBasedIndexExtension.class).getOrBuildCache(BY_INDEX_ID).get(indexId);
    }
}

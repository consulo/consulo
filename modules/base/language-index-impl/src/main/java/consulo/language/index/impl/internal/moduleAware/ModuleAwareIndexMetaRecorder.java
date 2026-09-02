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
import consulo.language.psi.stub.FileBasedIndex;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    private record ProviderStateCache(long rootsStamp, ConcurrentIntObjectMap<Map<String, OptionsMeta.PerProviderMeta>> byFile) {
    }

    private static final Map<ID<?, ?>, FileBasedIndexExtension<?, ?>> ourExtensionCache = new ConcurrentHashMap<>();
    private static final Key<AtomicReference<ProviderStateCache>> PROVIDER_STATE_CACHE = Key.create("module.aware.provider.state.cache");
    private static final Set<Integer> ourReindexRequested = ConcurrentHashMap.newKeySet();

    private ModuleAwareIndexMetaRecorder() {
    }

    /**
     * Read-path hook: returns {@code true} if stored {@link OptionsMeta} for
     * {@code (indexId, file)} no longer matches the current provider state, meaning the
     * cached index entry is stale for this module's options. Callers should drop the
     * cached result and trigger reindex via {@link #requestReindexOnce}.
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
            if (IndexingStamp.isFileIndexedStateCurrent(fileId, indexId)) {
                return true;
            }
            ourReindexRequested.remove(fileId);
            return false;
        }

        Map<String, OptionsMeta.PerProviderMeta> currentState = cachedState(project, fileId, file, module);
        boolean stale = OptionsRevalidator.needsReindex(extension.getVersion(), stored, idsOf(applicable), currentState);
        if (!stale) {
            ourReindexRequested.remove(fileId);
        }
        return stale;
    }

    /**
     * Fires {@link FileBasedIndex#requestReindex} at most once per staleness episode.
     * The guard clears when fresh meta lands ({@link #recordIfApplicable}) or when the
     * file is observed clean again ({@link #isStale} returning {@code false}).
     */
    public static boolean requestReindexOnce(VirtualFile file) {
        if (!(file instanceof VirtualFileWithId withId)) {
            return false;
        }
        if (ourReindexRequested.add(withId.getId())) {
            FileBasedIndex.getInstance().requestReindex(file);
            return true;
        }
        return false;
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
        Map<String, OptionsMeta.PerProviderMeta> freshState = OptionsRevalidator.currentState(allApplicable, module, file);

        OptionsMeta snapshot = OptionsRevalidator.snapshot(extension.getVersion(), idsOf(applicable), freshState);
        ModuleAwareIndexMetaStorage.getInstance().put(indexId, fileId, snapshot);

        cacheHolder(project).byFile().put(fileId, freshState);
        ourReindexRequested.remove(fileId);
    }

    private static Map<String, OptionsMeta.PerProviderMeta> cachedState(Project project, int fileId, VirtualFile file, Module module) {
        ProviderStateCache holder = cacheHolder(project);
        Map<String, OptionsMeta.PerProviderMeta> state = holder.byFile().get(fileId);
        if (state != null) {
            return state;
        }
        List<ModuleAwareIndexOptionProvider> allApplicable = ModuleAwareIndexOptionRegistry.getApplicableProviders(file.getFileType());
        state = OptionsRevalidator.currentState(allApplicable, module, file);
        Map<String, OptionsMeta.PerProviderMeta> existing = holder.byFile().putIfAbsent(fileId, state);
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
        FileBasedIndexExtension<?, ?> cached = ourExtensionCache.get(indexId);
        if (cached != null) {
            return cached;
        }
        for (FileBasedIndexExtension<?, ?> extension : FileBasedIndexExtension.EXTENSION_POINT_NAME.getExtensionList()) {
            if (extension.getName().equals(indexId)) {
                ourExtensionCache.put(indexId, extension);
                return extension;
            }
        }
        return null;
    }
}

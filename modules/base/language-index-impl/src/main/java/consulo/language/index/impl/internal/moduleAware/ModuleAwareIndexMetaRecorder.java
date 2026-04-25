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
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.ModuleAwareIndexOptionProvider;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
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

/**
 * Write-path hook: called by {@code FileBasedIndexImpl} after a successful
 * {@code updateSingleIndex(indexId, file, ...)} to record the current options
 * {@link OptionsMeta} for the {@code (indexId, file)} pair.
 *
 * <p>The listener side ({@code ModuleAwareIndexRootChangeListener}) reads back this meta
 * on {@code rootsChanged} to decide whether a reindex is needed. Together these two sides
 * give end-to-end options-drift detection.</p>
 *
 * <p>Fast path: if the extension is options-agnostic ({@code getOptionProviderIds}
 * empty), this is a no-op with one map lookup. Hot path for every file-index write,
 * so the extension-by-id cache is kept simple and lock-free.</p>
 */
public final class ModuleAwareIndexMetaRecorder {
    private static final Map<ID<?, ?>, FileBasedIndexExtension<?, ?>> ourExtensionCache = new ConcurrentHashMap<>();

    private ModuleAwareIndexMetaRecorder() {
    }

    /**
     * Read-path hook: returns {@code true} if stored {@link OptionsMeta} for
     * {@code (indexId, file)} no longer matches the current provider state, meaning the
     * cached index entry is stale for this module's options. Callers should drop the
     * cached result and trigger reindex.
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

        Module module = ProjectFileIndex.getInstance(project).getModuleForFile(file);
        if (module == null) {
            return false;
        }

        Set<String> requested = new HashSet<>(requestedIds);
        List<ModuleAwareIndexOptionProvider> applicable = new ArrayList<>();
        for (ModuleAwareIndexOptionProvider provider : ModuleAwareIndexOptionRegistry.getApplicableProviders(file.getFileType())) {
            if (requested.contains(provider.getId())) {
                applicable.add(provider);
            }
        }
        if (applicable.isEmpty()) {
            return false;
        }

        OptionsMeta stored = ModuleAwareIndexMetaStorage.getInstance().get(indexId, withId.getId());
        if (stored == null) {
            return false;
        }
        return OptionsRevalidator.needsReindex(extension.getVersion(), stored, applicable, module, file);
    }

    public static void recordIfApplicable(ID<?, ?> indexId,
                                          VirtualFile file,
                                          @Nullable Project project) {
        if (project == null || !(file instanceof VirtualFileWithId)) {
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

        FileType fileType = file.getFileType();
        Set<String> requested = new HashSet<>(requestedIds);
        List<ModuleAwareIndexOptionProvider> applicable = new ArrayList<>();
        for (ModuleAwareIndexOptionProvider provider : ModuleAwareIndexOptionRegistry.getApplicableProviders(fileType)) {
            if (requested.contains(provider.getId())) {
                applicable.add(provider);
            }
        }
        if (applicable.isEmpty()) {
            return;
        }

        int fileId = ((VirtualFileWithId) file).getId();
        OptionsMeta snapshot = OptionsRevalidator.snapshot(extension.getVersion(), applicable, module, file);
        ModuleAwareIndexMetaStorage.getInstance().put(indexId, fileId, snapshot);
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

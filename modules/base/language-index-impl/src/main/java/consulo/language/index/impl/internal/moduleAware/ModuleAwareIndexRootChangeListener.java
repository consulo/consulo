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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.content.ContentIterator;
import consulo.index.io.ID;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.ModuleAwareIndexOptionProvider;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Invalidation entry point: on {@code rootsChanged} iterate project files, diff stored
 * options-meta against current providers, and request reindex for any file whose options
 * no longer match what was indexed.
 *
 * <p>Runs off-EDT in smart mode on a pooled thread — project walks can be large. Reindex
 * uses {@link FileBasedIndex#requestReindex} which is coarse (reindexes all indexes for
 * the file). Targeted per-index reindex is a TODO once we have the internal API.</p>
 */
@TopicImpl(ComponentScope.PROJECT)
public final class ModuleAwareIndexRootChangeListener implements ModuleRootListener {
    private static final Logger LOG = Logger.getInstance(ModuleAwareIndexRootChangeListener.class);

    private final Project myProject;
    private final Provider<ModuleAwareIndexMetaStorage> myStorage;

    @Inject
    public ModuleAwareIndexRootChangeListener(Project project,
                                              Provider<ModuleAwareIndexMetaStorage> storage) {
        myProject = project;
        myStorage = storage;
    }

    @Override
    public void rootsChanged(ModuleRootEvent event) {
        myStorage.get().flush();

        List<FileBasedIndexExtension<?, ?>> optionsSensitive = collectOptionsSensitiveExtensions();
        if (optionsSensitive.isEmpty()) {
            return;
        }

        DumbService.getInstance(myProject).smartInvokeLater(() ->
            Application.get().executeOnPooledThread(() -> scan(optionsSensitive)));
    }

    private static List<FileBasedIndexExtension<?, ?>> collectOptionsSensitiveExtensions() {
        List<FileBasedIndexExtension<?, ?>> result = new ArrayList<>();
        for (FileBasedIndexExtension<?, ?> ext : FileBasedIndexExtension.EXTENSION_POINT_NAME.getExtensionList()) {
            if (!ext.getOptionProviderIds().isEmpty()) {
                result.add(ext);
            }
        }
        return result;
    }

    private void scan(List<FileBasedIndexExtension<?, ?>> optionsSensitive) {
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(myProject);
        ModuleAwareIndexMetaStorage storage = myStorage.get();
        FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();

        ContentIterator processor = file -> {
            if (!(file instanceof VirtualFileWithId withId)) {
                return true;
            }
            int fileId = withId.getId();
            Module module = fileIndex.getModuleForFile(file);
            if (module == null) {
                return true;
            }
            FileType fileType = file.getFileType();

            for (FileBasedIndexExtension<?, ?> ext : optionsSensitive) {
                revalidateOne(ext, fileId, file, module, fileType, storage, fileBasedIndex);
            }
            return true;
        };

        ReadAction.run(() -> fileIndex.iterateContent(processor));
    }

    private static void revalidateOne(FileBasedIndexExtension<?, ?> ext,
                                      int fileId,
                                      VirtualFile file,
                                      Module module,
                                      FileType fileType,
                                      ModuleAwareIndexMetaStorage storage,
                                      FileBasedIndex fileBasedIndex) {
        List<String> requestedIds = ext.getOptionProviderIds();
        if (requestedIds.isEmpty()) {
            return;
        }

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

        ID<?, ?> indexId = ext.getName();
        OptionsMeta stored = storage.get(indexId, fileId);
        if (stored == null) {
            return;
        }

        if (OptionsRevalidator.needsReindex(ext.getVersion(), stored, applicable, module, file)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Module-aware reindex: " + indexId + " for " + file);
            }
            fileBasedIndex.requestReindex(file);
            storage.delete(indexId, fileId);
        }
    }
}

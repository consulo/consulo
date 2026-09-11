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

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.document.util.FileContentUtilCore;
import consulo.language.index.impl.internal.moduleAware.IndexOptionHasher;
import consulo.language.index.impl.internal.moduleAware.ModuleAwareIndexMetaRecorder;
import consulo.language.index.impl.internal.moduleAware.ModuleAwareIndexOptionRegistry;
import consulo.language.index.impl.internal.moduleAware.ModuleAwareIndexOptionValueStorage;
import consulo.language.index.impl.internal.stub.SerializedStubTree;
import consulo.language.index.impl.internal.stub.StubUpdatingIndex;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.ModuleAwareIndexOptions;
import consulo.application.dumb.IndexNotReadyException;
import java.util.Map;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.language.internal.psi.stub.ModuleAwareIndexOptionsRescanner;
import consulo.language.psi.stub.IndexOption;
import consulo.language.psi.stub.ModuleAwareIndexOptionProvider;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Singleton
@ServiceImpl
public class ModuleAwareIndexOptionsRescannerImpl implements ModuleAwareIndexOptionsRescanner {
    @Override
    public byte @Nullable [] getRecordedOptionPayload(VirtualFile file, String providerId) {
        if (!(file instanceof VirtualFileWithId withId)) {
            return null;
        }
        ModuleAwareIndexOptionValueStorage.StoredOption stored =
            ModuleAwareIndexOptionValueStorage.getInstance().get(providerId, withId.getId());
        return stored == null ? null : stored.payload();
    }

    @Override
    public byte[] payloadOf(IndexOption option) {
        return IndexOptionHasher.payloadOf(option);
    }

    @Override
    public boolean applyViewOptions(Project project, VirtualFile file, @Nullable Map<String, byte[]> payloads) {
        if (payloads != null && !hasVariant(project, file, payloads)) {
            return false;
        }
        file.putUserData(ModuleAwareIndexOptions.VIEW_OPTIONS, payloads);
        Application application = Application.get();
        application.invokeLater(() -> {
            if (!project.isDisposed() && file.isValid()) {
                application.runWriteAction(() -> FileContentUtilCore.reparseFiles(List.of(file)));
            }
        });
        return true;
    }

    private static boolean hasVariant(Project project, VirtualFile file, Map<String, byte[]> payloads) {
        try {
            Map<Integer, SerializedStubTree> data = FileBasedIndex.getInstance().getFileData(StubUpdatingIndex.INDEX_ID, file, project);
            if (data.size() != 1) {
                return false;
            }
            return data.values().iterator().next().findVariant(payloads) >= 0;
        }
        catch (IndexNotReadyException e) {
            return false;
        }
    }

    @Override
    public @Nullable IndexOption getDefaultOptions(Project project, VirtualFile file, String providerId) {
        ModuleAwareIndexOptionProvider provider = ModuleAwareIndexOptionRegistry.findById(providerId);
        if (provider == null || project.isDisposed()) {
            return null;
        }
        Module module = ProjectFileIndex.getInstance(project).getModuleForFile(file);
        return module == null ? null : provider.getOptions(module, file);
    }

    @Override
    public void optionsChanged(Project project, Collection<VirtualFile> files, String reason) {
        if (project.isDisposed() || files.isEmpty()) {
            return;
        }
        ModuleAwareIndexMetaRecorder.dropCachedState(project, files);
        List<IndexableFilesIterator> iterators = new ArrayList<>(1);
        iterators.add(new OptionsChangedIndexableFilesIterator(new ArrayList<>(files), reason));
        new UnindexedFilesScanner(project, iterators, reason).queue();
    }
}

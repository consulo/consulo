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
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.logging.Logger;
import consulo.document.util.FileContentUtilCore;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.ArrayList;
import java.util.List;

/**
 * Invalidation entry point: on {@code rootsChanged} iterate project files, diff stored
 * options-meta against current providers, and request reindex for any file whose options
 * no longer match what was indexed. The staleness decision itself lives in
 * {@link ModuleAwareIndexMetaRecorder#isStale} — one implementation shared by the stub
 * read path and this bulk path.
 *
 * <p>Runs off-EDT in smart mode on a pooled thread — project walks can be large. Reindex
 * uses {@link ModuleAwareIndexMetaRecorder#requestReindexOnce} which reindexes all indexes
 * of the file, deliberately: drifted options change the file's interpretation, so indexes
 * not listing the provider may be stale too. Full-file reindex never under-invalidates and
 * matches the pushed-properties drift behaviour.</p>
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

        List<VirtualFile> stale = new ArrayList<>();
        ContentIterator processor = file -> {
            if (!(file instanceof VirtualFileWithId withId)) {
                return true;
            }

            for (FileBasedIndexExtension<?, ?> ext : optionsSensitive) {
                ID<?, ?> indexId = ext.getName();
                if (ModuleAwareIndexMetaRecorder.isStale(indexId, file, myProject)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Module-aware reindex: " + indexId + " for " + file);
                    }
                    ModuleAwareIndexMetaRecorder.requestReindexOnce(file);
                    storage.delete(indexId, withId.getId());
                    stale.add(file);
                }
            }
            return true;
        };

        ReadAction.run(() -> fileIndex.iterateContent(processor));

        if (!stale.isEmpty()) {
            // options may steer the parse itself (preprocessor seeding), so a drifted file
            // needs its tree rebuilt, not only its index entries
            Application application = Application.get();
            application.invokeLater(() -> application.runWriteAction(() -> FileContentUtilCore.reparseFiles(stale)));
        }
    }
}

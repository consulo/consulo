/*
 * Copyright 2013-2016 consulo.io
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
package consulo.module.impl.internal;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableModuleRootLayer;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.AsyncFileListener;
import consulo.virtualFileSystem.event.VFileDeleteEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.*;

/**
 * @author VISTALL
 * @since 06.04.2015
 */
@SuppressWarnings("ExtensionImplIsNotAnnotated")
public class ContentEntryDeleteFileListener implements AsyncFileListener {
    private final ProjectLocator myProjectLocator;
    private final Project myProject;

    @Inject
    public ContentEntryDeleteFileListener(ProjectLocator projectLocator, Project project) {
        myProjectLocator = projectLocator;
        myProject = project;
    }

    @Nullable
    @Override
    public ChangeApplier prepareChange(@Nonnull List<? extends VFileEvent> events) {
        Map<Module, Set<VirtualFile>> deletedFiles = new HashMap<>();

        for (VFileEvent event : events) {
            if (event instanceof VFileDeleteEvent) {
                VirtualFile fileToDelete = event.getFile();
                Project project = fileToDelete == null ? null : myProjectLocator.guessProjectForFile(fileToDelete);
                if (project == null || myProject != project) {
                    continue;
                }

                Module moduleForFile = ModuleUtilCore.findModuleForFile(fileToDelete, project);

                // if module have dir url - dont need check
                if (moduleForFile == null || moduleForFile.getModuleDirUrl() != null) {
                    continue;
                }

                deletedFiles.computeIfAbsent(moduleForFile, module -> new HashSet<>()).add(fileToDelete);
            }
        }

        if (deletedFiles.isEmpty()) {
            return null;
        }

        return new ChangeApplier() {
            @Override
            public void beforeVfsChange() {
                for (Map.Entry<Module, Set<VirtualFile>> entry : deletedFiles.entrySet()) {
                    Module module = entry.getKey();
                    Set<VirtualFile> files = entry.getValue();

                    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

                    boolean removed = false;

                    ModifiableRootModel modifiableModel = ReadAction.compute(moduleRootManager::getModifiableModel);

                    for (ModuleRootLayer layer : modifiableModel.getLayers().values()) {
                        ModifiableModuleRootLayer modifiableModuleRootLayer = (ModifiableModuleRootLayer) layer;

                        for (ContentEntry contentEntry : modifiableModuleRootLayer.getContentEntries()) {
                            if (files.contains(contentEntry.getFile())) {
                                modifiableModuleRootLayer.removeContentEntry(contentEntry);

                                removed = true;
                            }
                        }
                    }

                    if (removed) {
                        Application application = module.getApplication();
                        application.invokeLater(() -> WriteAction.run(modifiableModel::commit), application.getAnyModalityState());
                    }
                    else {
                        modifiableModel.dispose();
                    }
                }
            }
        };
    }
}

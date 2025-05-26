/*
 * Copyright 2013-2025 consulo.io
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
package consulo.project.impl.internal;

import consulo.application.WriteAction;
import consulo.content.base.ExcludedContentFolderTypeProvider;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ProjectOpenContext;
import consulo.project.internal.ProjectOpenProcessor;
import consulo.ui.UIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * @author VISTALL
 * @since 2025-05-26
 */
public class FolderProjectOpenProcessor extends ProjectOpenProcessor {
    public static final FolderProjectOpenProcessor INSTANCE = new FolderProjectOpenProcessor();

    @Nonnull
    @Override
    public String getFileSample() {
        return "Folder";
    }

    @Nullable
    @Override
    public Image getIcon(@Nonnull VirtualFile file) {
        return PlatformIconGroup.nodesFolder();
    }

    @Override
    public boolean canOpenProject(@Nonnull File file) {
        return true;
    }

    // TODO unify with NewOrImportModuleUtil ?
    @Nonnull
    @Override
    public AsyncResult<Project> doOpenProjectAsync(@Nonnull VirtualFile virtualFile, @Nonnull UIAccess uiAccess, @Nonnull ProjectOpenContext context) {
        final ProjectManager projectManager = ProjectManager.getInstance();
        final String projectFilePath = virtualFile.getPath();
        String projectName = virtualFile.getName();

        AsyncResult<Project> result = AsyncResult.undefined();

        uiAccess.give(() -> {
            try {
                File projectDir = new File(projectFilePath);
                FileUtil.ensureExists(projectDir);
                File projectConfigDir = new File(projectDir, Project.DIRECTORY_STORE_FOLDER);
                FileUtil.ensureExists(projectConfigDir);

                Project newProject = projectManager.createProject(projectName, projectFilePath);

                if (newProject == null) {
                    result.rejectWithThrowable(new IllegalArgumentException("Project not initialized"));
                    return;
                }

                newProject.save();

                ModifiableModuleModel modifiableModel = ModuleManager.getInstance(newProject).getModifiableModel();

                Module newModule = modifiableModel.newModule(projectName, virtualFile.getPath());

                ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(newModule);
                ModifiableRootModel modifiableModelForModule = moduleRootManager.getModifiableModel();
                ContentEntry contentEntry = modifiableModelForModule.addContentEntry(virtualFile);

                contentEntry.addFolder(contentEntry.getUrl() + "/" + Project.DIRECTORY_STORE_FOLDER, ExcludedContentFolderTypeProvider.getInstance());

                WriteAction.runAndWait(() -> {
                    modifiableModelForModule.commit();

                    modifiableModel.commit();
                });

                newProject.save();

                ProjectManager.getInstance().openProjectAsync(newProject, uiAccess).notify(result);
            }
            catch (IOException e) {
                result.rejectWithThrowable(e);
            }
        });

        return result;
    }
}

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
package consulo.project.impl.internal;

import consulo.application.concurrent.coroutine.ReadLock;
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.content.base.ExcludedContentFolderTypeProvider;
import consulo.disposer.Disposer;
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
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * @author VISTALL
 * @since 2025-05-26
 */
public class FolderProjectOpenProcessor extends ProjectOpenProcessor {
    public static final FolderProjectOpenProcessor INSTANCE = new FolderProjectOpenProcessor();

    private record ProjectContext(
        VirtualFile virtualFile,
        Project project
    ) {
    }

    private record FolderSetupContext(
        VirtualFile virtualFile,
        Project project,
        ModifiableModuleModel modifiableModel,
        ModifiableRootModel modifiableModelForModule
    ) {
    }

    @Override
    public @Nullable Image getIcon(VirtualFile file) {
        return PlatformIconGroup.nodesFolder();
    }

    @Override
    public boolean canOpenProject(File file) {
        return true;
    }

    @Override
    public <I> Coroutine<I, VirtualFile> prepareSteps(UIAccess uiAccess,
                                                       ProjectOpenContext context,
                                                       Coroutine<I, VirtualFile> in) {
        return in
            // Create directories + temp project
            .then(CodeExecution.<VirtualFile, ProjectContext>apply(virtualFile -> {
                String projectFilePath = virtualFile.getPath();
                String projectName = virtualFile.getName();

                File projectDir = new File(projectFilePath);
                try {
                    FileUtil.ensureExists(projectDir);
                    FileUtil.ensureExists(new File(projectDir, Project.DIRECTORY_STORE_FOLDER));
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }

                ProjectManager projectManager = ProjectManager.getInstance();
                Project newProject = projectManager.createProject(projectName, projectFilePath);

                if (newProject == null) {
                    throw new IllegalStateException("Project not initialized");
                }

                newProject.save(uiAccess);

                return new ProjectContext(virtualFile, newProject);
            }))
            // Get modifiable models under read lock
            .then(ReadLock.<ProjectContext, FolderSetupContext>apply(ctx -> {
                ModifiableModuleModel modifiableModel = ModuleManager.getInstance(ctx.project()).getModifiableModel();
                Module newModule = modifiableModel.newModule(ctx.virtualFile().getName(), ctx.virtualFile().getPath());

                ModifiableRootModel modifiableModelForModule = ModuleRootManager.getInstance(newModule).getModifiableModel();
                ContentEntry contentEntry = modifiableModelForModule.addContentEntry(ctx.virtualFile());
                contentEntry.addFolder(contentEntry.getUrl() + "/" + Project.DIRECTORY_STORE_FOLDER, ExcludedContentFolderTypeProvider.getInstance());

                return new FolderSetupContext(ctx.virtualFile(), ctx.project(), modifiableModel, modifiableModelForModule);
            }))
            // Commit models under write lock
            .then(WriteLock.<FolderSetupContext, FolderSetupContext>apply(ctx -> {
                ctx.modifiableModelForModule().commit();
                ctx.modifiableModel().commit();
                return ctx;
            }))
            // Save and cleanup, return original VirtualFile
            .then(CodeExecution.<FolderSetupContext, VirtualFile>apply(ctx -> {
                ctx.project().save(uiAccess);
                Disposer.dispose(ctx.project());
                return ctx.virtualFile();
            }));
    }
}

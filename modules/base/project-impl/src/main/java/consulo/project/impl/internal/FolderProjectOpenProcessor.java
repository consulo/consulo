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
import consulo.util.concurrent.coroutine.step.CallSubroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.dataholder.Key;
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

    private static final Key<VirtualFile> VIRTUAL_FILE = Key.create("FolderProjectOpenProcessor.virtualFile");
    private static final Key<Project> PROJECT = Key.create("FolderProjectOpenProcessor.project");
    private static final Key<ModifiableModuleModel> MODULE_MODEL = Key.create("FolderProjectOpenProcessor.moduleModel");
    private static final Key<ModifiableRootModel> ROOT_MODEL = Key.create("FolderProjectOpenProcessor.rootModel");

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
            .then(CodeExecution.<VirtualFile, Object>apply((virtualFile, continuation) -> {
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

                Project newProject = ProjectManager.getInstance().createProject(projectName, projectFilePath);

                if (newProject == null) {
                    throw new IllegalStateException("Project not initialized");
                }

                continuation.putCopyableUserData(VIRTUAL_FILE, virtualFile);
                continuation.putCopyableUserData(PROJECT, newProject);
                return null;
            }))
            // Save the freshly created project as a subroutine
            .then(CallSubroutine.<Object, Object>call(c -> c.getCopyableUserData(PROJECT).saveAsync(uiAccess)))
            // Get modifiable models under read lock
            .then(ReadLock.<Object, Object>apply((o, c) -> {
                Project project = c.getCopyableUserData(PROJECT);
                VirtualFile virtualFile = c.getCopyableUserData(VIRTUAL_FILE);

                ModifiableModuleModel modifiableModel = ModuleManager.getInstance(project).getModifiableModel();
                Module newModule = modifiableModel.newModule(virtualFile.getName(), virtualFile.getPath());

                ModifiableRootModel modifiableModelForModule = ModuleRootManager.getInstance(newModule).getModifiableModel();
                ContentEntry contentEntry = modifiableModelForModule.addContentEntry(virtualFile);
                contentEntry.addFolder(contentEntry.getUrl() + "/" + Project.DIRECTORY_STORE_FOLDER, ExcludedContentFolderTypeProvider.getInstance());

                c.putCopyableUserData(MODULE_MODEL, modifiableModel);
                c.putCopyableUserData(ROOT_MODEL, modifiableModelForModule);
                return o;
            }))
            // Commit models under write lock
            .then(WriteLock.<Object, Object>apply((o, c) -> {
                c.getCopyableUserData(ROOT_MODEL).commit();
                c.getCopyableUserData(MODULE_MODEL).commit();
                return o;
            }))
            // Save again as a subroutine
            .then(CallSubroutine.<Object, Object>call(c -> c.getCopyableUserData(PROJECT).saveAsync(uiAccess)))
            // Cleanup, return original VirtualFile
            .then(WriteLock.<Object, VirtualFile>apply((o, c) -> {
                Disposer.dispose(c.getCopyableUserData(PROJECT));
                return c.getCopyableUserData(VIRTUAL_FILE);
            }));
    }
}

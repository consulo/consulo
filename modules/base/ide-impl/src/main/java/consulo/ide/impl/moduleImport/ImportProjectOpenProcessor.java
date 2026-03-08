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
package consulo.ide.impl.moduleImport;

import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.ide.moduleImport.ModuleImportContext;
import consulo.ide.moduleImport.ModuleImportProcessor;
import consulo.ide.moduleImport.ModuleImportProvider;
import consulo.ide.moduleImport.ModuleImportProviders;
import consulo.ide.newModule.NewOrImportModuleUtil;
import consulo.project.Project;
import consulo.project.ProjectOpenContext;
import consulo.project.impl.internal.DefaultProjectOpenProcessor;
import consulo.project.internal.ProjectOpenProcessor;
import consulo.ui.Alert;
import consulo.ui.UIAccess;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.AsyncResult;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import consulo.util.io.FileUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 31-Jan-17
 */
public class ImportProjectOpenProcessor extends ProjectOpenProcessor {
    private final List<ModuleImportProvider> myProviders;

    public ImportProjectOpenProcessor() {
        myProviders = ModuleImportProviders.getExtensions(false);
    }

    @Nullable
    @Override
    public Image getIcon(@Nonnull VirtualFile file) {
        File ioFile = VfsUtil.virtualToIoFile(file);
        for (ModuleImportProvider provider : myProviders) {
            if (provider.canImport(ioFile)) {
                return provider.getIcon();
            }
        }
        return null;
    }

    @Override
    public boolean canOpenProject(@Nonnull File file) {
        for (ModuleImportProvider provider : myProviders) {
            if (provider.canImport(file)) {
                return true;
            }
        }

        return false;
    }

    @Nonnull
    @Override
    public <I> Coroutine<I, VirtualFile> prepareSteps(@Nonnull UIAccess uiAccess,
                                                       @Nonnull ProjectOpenContext openContext,
                                                       @Nonnull Coroutine<I, VirtualFile> in) {
        return in
            // Show "open existing or reimport?" dialog + run import if needed
            .then(CompletableFutureStep.<VirtualFile, VirtualFile>await(virtualFile -> {
                File ioPath = VfsUtil.virtualToIoFile(virtualFile);

                List<ModuleImportProvider> targetProviders =
                    ContainerUtil.filter(myProviders, moduleImportProvider -> moduleImportProvider.canImport(ioPath));

                if (targetProviders.isEmpty()) {
                    throw new IllegalArgumentException("must be not empty");
                }

                String expectedProjectPath = ModuleImportProvider.getDefaultPath(virtualFile);

                CompletableFuture<VirtualFile> future = new CompletableFuture<>();

                // If existing project and not headless — ask what to do
                if (!uiAccess.isHeadless() && DefaultProjectOpenProcessor.getInstance().canOpenProject(new File(expectedProjectPath))) {
                    askOpenOrReimport(virtualFile, ioPath, expectedProjectPath, targetProviders, uiAccess, future);
                }
                else {
                    // Headless or no existing project — go straight to import
                    runImport(virtualFile, targetProviders, uiAccess, future);
                }

                return future;
            }))
            // Cancel check (null means user cancelled)
            .then(CodeExecution.<VirtualFile, VirtualFile>apply((vf, continuation) -> {
                if (vf == null) {
                    continuation.cancel();
                    return null;
                }
                return vf;
            }));
    }

    private void askOpenOrReimport(@Nonnull VirtualFile virtualFile,
                                   @Nonnull File ioPath,
                                   @Nonnull String expectedProjectPath,
                                   @Nonnull List<ModuleImportProvider> targetProviders,
                                   @Nonnull UIAccess uiAccess,
                                   @Nonnull CompletableFuture<VirtualFile> future) {
        Alert<ThreeState> alert = Alert.create();
        alert.title(IdeLocalize.titleOpenProject());
        alert.text(IdeLocalize.projectImportOpenExisting(
            "an existing project",
            FileUtil.toSystemDependentName(ioPath.getPath()),
            virtualFile.getName()
        ));
        alert.asQuestion();

        alert.button(IdeLocalize.projectImportOpenExistingOpenexisting().get(), ThreeState.YES);
        alert.asDefaultButton();

        alert.button(IdeLocalize.projectImportOpenExistingReimport().get(), ThreeState.NO);

        alert.button(Alert.CANCEL, ThreeState.UNSURE);
        alert.asExitButton();

        uiAccess.give(() -> alert.showAsync().doWhenDone(threeState -> {
            switch (threeState) {
                case YES:
                    // Open existing project — resolve VirtualFile for the project directory
                    VirtualFile projectDir = LocalFileSystem.getInstance().refreshAndFindFileByPath(expectedProjectPath);
                    future.complete(projectDir);
                    break;
                case NO:
                    runImport(virtualFile, targetProviders, uiAccess, future);
                    break;
                case UNSURE:
                    future.complete(null);
                    break;
            }
        }));
    }

    private void runImport(@Nonnull VirtualFile virtualFile,
                           @Nonnull List<ModuleImportProvider> targetProviders,
                           @Nonnull UIAccess uiAccess,
                           @Nonnull CompletableFuture<VirtualFile> future) {
        uiAccess.give(() -> {
            AsyncResult<Pair<ModuleImportContext, ModuleImportProvider<ModuleImportContext>>> result = AsyncResult.undefined();
            ModuleImportProcessor.showImportChooser(null, virtualFile, targetProviders, result);

            result.doWhenDone(pair -> {
                ModuleImportContext context = pair.getFirst();

                ModuleImportProvider<ModuleImportContext> provider = pair.getSecond();
                AsyncResult<Project> importProjectAsync = NewOrImportModuleUtil.importProject(context, provider);
                importProjectAsync.doWhenDone((newProject) -> {
                    VirtualFile projectDir = LocalFileSystem.getInstance().refreshAndFindFileByPath(newProject.getBasePath());

                    // Dispose the temp project — ProjectOpenService will create a real one
                    Disposer.dispose(newProject);

                    future.complete(projectDir);
                });

                importProjectAsync.doWhenRejected((Runnable) () -> future.complete(null));
            });

            result.doWhenRejected((pair, error) -> {
                if (pair != null) {
                    pair.getFirst().dispose();
                }

                future.complete(null);
            });
        });
    }
}

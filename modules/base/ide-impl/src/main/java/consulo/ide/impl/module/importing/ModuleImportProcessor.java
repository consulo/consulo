/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ide.impl.module.importing;

import consulo.application.Application;
import consulo.application.ApplicationPropertiesComponent;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.localize.LocalizeValue;
import consulo.module.creation.importing.ModuleImportContext;
import consulo.module.creation.importing.ModuleImportProvider;
import consulo.project.Project;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2019-08-26
 */
public class ModuleImportProcessor {
    private static final String LAST_IMPORTED_LOCATION = "last.imported.location";

    /**
     * Will execute module importing. Will show popup for selecting import providers if more that one, and then show import wizard
     *
     * @param project - null mean its new project creation
     * @return
     */
    @RequiredUIAccess
    public static <C extends ModuleImportContext> CompletableFuture<Pair<C, ModuleImportProvider<C>>> showFileChooser(
        @Nullable Project project,
        @Nullable FileChooserDescriptor chooserDescriptor
    ) {
        boolean isModuleImport = project != null;

        FileChooserDescriptor descriptor = ObjectUtil.notNull(chooserDescriptor, createAllImportDescriptor(isModuleImport));

        Path toSelect = null;
        String lastLocation = ApplicationPropertiesComponent.getInstance().getValue(LAST_IMPORTED_LOCATION);
        if (lastLocation != null) {
            toSelect = Path.of(lastLocation);
        }

        CompletableFuture<Pair<C, ModuleImportProvider<C>>> result = new CompletableFuture<>();

        UIAccess uiAccess = UIAccess.current();

        FileChooser.choosePath(descriptor, project, toSelect).whenCompleteAsync((f, error) -> {
            if (error != null) {
                result.completeExceptionally(error);
                return;
            }

            ApplicationPropertiesComponent.getInstance().setValue(LAST_IMPORTED_LOCATION, f.toString());

            showImportChooser(project, f, result);
        }, uiAccess);

        return result;
    }

    private static FileChooserDescriptor createAllImportDescriptor(boolean isModuleImport) {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, true, true, false, false) {
            @Override
            public Image getIcon(Path file) {
                for (ModuleImportProvider importProvider : ModuleImportProviders.getExtensions(isModuleImport)) {
                    if (importProvider.canImport(file)) {
                        return importProvider.getIcon();
                    }
                }
                return super.getIcon(file);
            }
        };
        descriptor.setHideIgnored(false);
        descriptor.setTitle("Select File or Directory to Import");
        return descriptor;
    }

    @RequiredUIAccess
    public static <C extends ModuleImportContext> void showImportChooser(
        @Nullable Project project,
        Path fileOrDir,
        CompletableFuture<Pair<C, ModuleImportProvider<C>>> result
    ) {
        boolean isModuleImport = project != null;

        List<ModuleImportProvider> providers = ModuleImportProviders.getExtensions(isModuleImport);

        List<ModuleImportProvider> availableProviders = ContainerUtil.filter(providers, provider -> provider.canImport(fileOrDir));
        if (availableProviders.isEmpty()) {
            MessageBoxes.okError(
                LocalizeValue.localizeTODO("Cannot import anything from '" + FileUtil.toSystemDependentName(fileOrDir.toString()) + "'")
            ).showAsync();
            result.completeExceptionally(new CancellationException());
            return;
        }

        showImportChooser(project, fileOrDir, providers, result);
    }

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    public static <C extends ModuleImportContext> void showImportChooser(
        @Nullable Project project,
        Path file,
        List<ModuleImportProvider> providers,
        CompletableFuture<Pair<C, ModuleImportProvider<C>>> result
    ) {
        if (providers.size() == 1) {
            showImportWizard(project, file, providers.get(0), result);
        }
        else {
            showImportTarget(providers).whenComplete((provider, error) -> {
                if (error != null) {
                    result.completeExceptionally(error);
                }
                else {
                    showImportWizard(project, file, provider, result);
                }
            });
        }
    }

    @RequiredUIAccess
    private static CompletableFuture<ModuleImportProvider> showImportTarget(List<ModuleImportProvider> providers) {
        ModuleImportTargetDialogDescriptor descriptor = new ModuleImportTargetDialogDescriptor(providers);

        return Application.get().getInstance(DialogService.class)
            .build(descriptor)
            .showAsync()
            .thenApply(_ -> descriptor.getProvider());
    }

    @RequiredUIAccess
    private static <C extends ModuleImportContext> void showImportWizard(
        @Nullable Project project,
        Path targetFile,
        ModuleImportProvider<C> moduleImportProvider,
        CompletableFuture<Pair<C, ModuleImportProvider<C>>> result
    ) {
        ModuleImportDialogDescriptor<C> descriptor = new ModuleImportDialogDescriptor<>(project, targetFile, moduleImportProvider);

        Application.get().getInstance(DialogService.class).build(descriptor).showAsync().whenComplete((value, error) -> {
            if (error == null) {
                result.complete(Pair.create(descriptor.getContext(), moduleImportProvider));
            }
            else {
                descriptor.disposeContext();

                result.completeExceptionally(error);
            }
        });
    }
}

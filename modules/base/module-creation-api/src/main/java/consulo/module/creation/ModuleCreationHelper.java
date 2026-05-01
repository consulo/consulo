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
package consulo.module.creation;

import consulo.annotation.UsedInPlugin;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.module.creation.importing.ModuleImportContext;
import consulo.module.creation.importing.ModuleImportProvider;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-05-01
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ModuleCreationHelper {
    @RequiredUIAccess
    @UsedInPlugin
    <C extends ModuleImportContext> AsyncResult<Pair<C, ModuleImportProvider<C>>> showImportFileChooser(
        @Nullable Project project,
        @Nullable FileChooserDescriptor chooserDescriptor
    );

    @RequiredUIAccess
    @UsedInPlugin
    <C extends ModuleImportContext> void showImportChooser(
        @Nullable Project project,
        VirtualFile file,
        AsyncResult<Pair<C, ModuleImportProvider<C>>> result
    );

    @RequiredUIAccess
    @UsedInPlugin
    <C extends ModuleImportContext> void showImportChooser(
        @Nullable Project project,
        VirtualFile file,
        List<ModuleImportProvider> providers,
        AsyncResult<Pair<C, ModuleImportProvider<C>>> result
    );
}

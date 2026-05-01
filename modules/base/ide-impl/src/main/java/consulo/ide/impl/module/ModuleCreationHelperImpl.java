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
package consulo.ide.impl.module;

import consulo.annotation.component.ServiceImpl;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.impl.module.importing.ModuleImportProcessor;
import consulo.module.creation.ModuleCreationHelper;
import consulo.module.creation.importing.ModuleImportContext;
import consulo.module.creation.importing.ModuleImportProvider;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-05-01
 */
@Singleton
@ServiceImpl
public class ModuleCreationHelperImpl implements ModuleCreationHelper {
    @RequiredUIAccess
    @Override
    public <C extends ModuleImportContext> AsyncResult<Pair<C, ModuleImportProvider<C>>> showImportFileChooser(Project project,
                                                                                                               FileChooserDescriptor chooserDescriptor) {
        return ModuleImportProcessor.showFileChooser(project, chooserDescriptor);
    }

    @RequiredUIAccess
    @Override
    public <C extends ModuleImportContext> void showImportChooser(Project project,
                                                                  VirtualFile file,
                                                                  AsyncResult<Pair<C, ModuleImportProvider<C>>> result) {
        ModuleImportProcessor.showImportChooser(project, file, result);
    }

    @RequiredUIAccess
    @Override
    public <C extends ModuleImportContext> void showImportChooser(Project project,
                                                                  VirtualFile file,
                                                                  List<ModuleImportProvider> providers,
                                                                  AsyncResult<Pair<C, ModuleImportProvider<C>>> result) {
        ModuleImportProcessor.showImportChooser(project, file, providers, result);
    }
}

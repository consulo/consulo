/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.moduleImport;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.ide.newModule.ui.UnifiedProjectOrModuleNameStep;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.ex.wizard.WizardStep;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import org.intellij.lang.annotations.Language;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2017-01-30
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ModuleImportProvider<C extends ModuleImportContext> {
    ExtensionPointName<ModuleImportProvider> EP_NAME = ExtensionPointName.create(ModuleImportProvider.class);

    @SuppressWarnings("unchecked")
    @Nonnull
    default C createContext(@Nullable Project project) {
        return (C)new ModuleImportContext(project);
    }

    /**
     * If return false - this provider will be avaliable from 'Import Module' action from project structore
     */
    default boolean isOnlyForNewImport() {
        return true;
    }

    @Nonnull
    abstract String getName();

    @Nonnull
    abstract Image getIcon();

    boolean canImport(@Nonnull File fileOrDirectory);

    @RequiredReadAction
    void process(
        @Nonnull C context,
        @Nonnull Project project,
        @Nonnull ModifiableModuleModel model,
        @Nonnull Consumer<Module> newModuleConsumer
    );

    default String getPathToBeImported(@Nonnull VirtualFile file) {
        return getDefaultPath(file);
    }

    static String getDefaultPath(@Nonnull VirtualFile file) {
        return file.isDirectory() ? file.getPath() : file.getParent().getPath();
    }

    default void buildSteps(@Nonnull Consumer<WizardStep<C>> consumer, @Nonnull C context) {
        consumer.accept(new UnifiedProjectOrModuleNameStep<>(context));
    }

    @Nonnull
    @Language("HTML")
    default String getFileSample() {
        return getName();
    }
}

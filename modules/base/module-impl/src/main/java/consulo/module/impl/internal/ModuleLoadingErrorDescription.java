/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.annotation.DeprecationInfo;
import consulo.application.WriteAction;
import consulo.localize.LocalizeValue;
import consulo.module.ConfigurationErrorDescription;
import consulo.module.ConfigurationErrorType;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class ModuleLoadingErrorDescription extends ConfigurationErrorDescription {
    private static final ConfigurationErrorType INVALID_MODULE =
        new ConfigurationErrorType(ProjectLocalize.elementKindNameModule().get(), false);
    private final ModuleManagerImpl.ModuleLoadItem moduleLoadItem;
    private final ModuleManagerImpl myModuleManager;

    private ModuleLoadingErrorDescription(
        @Nonnull LocalizeValue description,
        ModuleManagerImpl.ModuleLoadItem modulePath,
        ModuleManagerImpl moduleManager,
        String elementName
    ) {
        super(elementName, description, INVALID_MODULE);
        moduleLoadItem = modulePath;
        myModuleManager = moduleManager;
    }

    public ModuleManagerImpl.ModuleLoadItem getModuleLoadItem() {
        return moduleLoadItem;
    }

    @Override
    @RequiredUIAccess
    public void ignoreInvalidElement() {
        ModifiableModuleModel modifiableModel = myModuleManager.getModifiableModel();
        Module module = modifiableModel.findModuleByName(moduleLoadItem.getName());
        if (module != null) {
            modifiableModel.disposeModule(module);
        }

        WriteAction.run(modifiableModel::commit);

        myModuleManager.removeFailedModulePath(moduleLoadItem);
    }

    @Override
    public LocalizeValue getIgnoreConfirmationMessage() {
        return ProjectLocalize.moduleRemoveFromProjectConfirmation(getElementName());
    }

    public static ModuleLoadingErrorDescription create(
        @Nonnull LocalizeValue description,
        ModuleManagerImpl.ModuleLoadItem loadItem,
        ModuleManagerImpl moduleManager
    ) {
        return new ModuleLoadingErrorDescription(description, loadItem, moduleManager, loadItem.getName());
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public static ModuleLoadingErrorDescription create(
        String description,
        ModuleManagerImpl.ModuleLoadItem loadItem,
        ModuleManagerImpl moduleManager
    ) {
        return new ModuleLoadingErrorDescription(LocalizeValue.ofNullable(description), loadItem, moduleManager, loadItem.getName());
    }
}

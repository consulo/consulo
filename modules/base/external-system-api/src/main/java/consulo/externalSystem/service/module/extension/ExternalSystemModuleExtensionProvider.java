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
package consulo.externalSystem.service.module.extension;

import consulo.annotation.UsedInPlugin;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 26/12/2025
 */
@UsedInPlugin
public abstract class ExternalSystemModuleExtensionProvider implements ModuleExtensionProvider<ExternalSystemModuleExtensionImpl> {
    private final ProjectSystemId myProjectSystemId;

    public ExternalSystemModuleExtensionProvider(ProjectSystemId projectSystemId) {
        myProjectSystemId = projectSystemId;
    }

    @Nonnull
    @Override
    public String getId() {
        return myProjectSystemId.getId();
    }

    @Override
    public boolean isSystemOnly() {
        return true;
    }

    @Nonnull
    @Override
    public LocalizeValue getName() {
        return myProjectSystemId.getDisplayName();
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return myProjectSystemId.getIcon();
    }

    @Nonnull
    @Override
    public ModuleExtension<ExternalSystemModuleExtensionImpl> createImmutableExtension(@Nonnull ModuleRootLayer moduleRootLayer) {
        return new ExternalSystemModuleExtensionImpl(getId(), moduleRootLayer, myProjectSystemId);
    }

    @Nonnull
    @Override
    public MutableModuleExtension<ExternalSystemModuleExtensionImpl> createMutableExtension(@Nonnull ModuleRootLayer moduleRootLayer) {
        return new ExternalSystemMutableModuleExtensionImpl(getId(), moduleRootLayer, myProjectSystemId);
    }
}

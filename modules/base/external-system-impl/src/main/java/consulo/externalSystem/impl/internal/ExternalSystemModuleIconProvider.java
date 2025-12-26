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
package consulo.externalSystem.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalSystem.service.module.extension.ExternalSystemModuleExtension;
import consulo.module.Module;
import consulo.module.content.ModuleIconProvider;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 26/12/2025
 */
@ExtensionImpl
public class ExternalSystemModuleIconProvider implements ModuleIconProvider {
    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public Image getIcon(@Nonnull Module module) {
        ExternalSystemModuleExtension<?> extension = module.getExtension(ExternalSystemModuleExtension.class);
        if (extension != null) {
            return extension.getProjectSystemId().getModuleIcon();
        }
        return null;
    }
}

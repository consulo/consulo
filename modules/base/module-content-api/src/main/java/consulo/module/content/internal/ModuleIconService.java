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
package consulo.module.content.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.module.Module;
import consulo.module.content.ModuleIconProvider;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.IconDeferrer;
import consulo.ui.ex.internal.AnyIconKey;
import consulo.ui.image.Image;
import consulo.ui.image.ImageKey;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 26/12/2025
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class ModuleIconService {
    @Nonnull
    public static ModuleIconService getInstance(@Nonnull Project project) {
        return project.getInstance(ModuleIconService.class);
    }

    private final IconDeferrer myIconDeferrer;
    private final Project myProject;

    @Inject
    public ModuleIconService(IconDeferrer iconDeferrer, Project project) {
        myIconDeferrer = iconDeferrer;
        myProject = project;
    }

    @Nonnull
    public Image getIcon(@Nonnull Module module) {
        ImageKey baseIcon = PlatformIconGroup.nodesModule();
        if (module.isDisposed()) {
            return baseIcon;
        }

        return myIconDeferrer.defer(baseIcon,
            new AnyIconKey<>(module, myProject, 0),
            k -> requestIcon(k.getObject())
        );
    }

    @Nonnull
    private Image requestIcon(@Nonnull Module module) {
        Image image = myProject.getExtensionPoint(ModuleIconProvider.class).computeSafeIfAny(it -> it.getIcon(module));
        if (image != null) {
            return image;
        }
        return PlatformIconGroup.nodesModule();
    }
}

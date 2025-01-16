/*
 * Copyright 2013-2016 consulo.io
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
package consulo.language.content;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.component.extension.ExtensionInstance;
import consulo.content.ContentFolderTypeProvider;
import consulo.content.ResourceLikeContentFolderTypeProvider;
import consulo.project.ProjectBundle;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 22:37/31.10.13
 */
@ExtensionImpl(id = "production-resource", order = "after production")
public class ProductionResourceContentFolderTypeProvider extends ContentFolderTypeProvider implements ResourceLikeContentFolderTypeProvider {
    private static final Supplier<ProductionResourceContentFolderTypeProvider> INSTANCE =
        ExtensionInstance.from(ContentFolderTypeProvider.class
        );

    @Nonnull
    public static ProductionResourceContentFolderTypeProvider getInstance() {
        return INSTANCE.get();
    }

    public ProductionResourceContentFolderTypeProvider() {
        super("PRODUCTION_RESOURCE");
    }

    @Override
    public int getWeight() {
        return 100;
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return AllIcons.Modules.ResourcesRoot;
    }

    @Nonnull
    @Override
    public String getName() {
        return ProjectBundle.message("module.toggle.resources.action");
    }

    @Nonnull
    @Override
    public ColorValue getGroupColor() {
        return new RGBColor(129, 45, 243);
    }
}

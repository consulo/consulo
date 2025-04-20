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

import consulo.application.Application;
import consulo.content.ContentFolderPropertyProvider;
import consulo.content.ContentFolderTypeProvider;
import consulo.content.base.ExcludedContentFolderTypeProvider;
import consulo.language.icon.IconDescriptor;
import consulo.module.content.layer.ContentFolderSupportPatcher;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2013-11-07
 */
public class ContentFoldersSupportUtil {
    @Nonnull
    public static Set<ContentFolderTypeProvider> getSupportedFolders(ModifiableRootModel moduleRootManager) {
        Set<ContentFolderTypeProvider> providers = new LinkedHashSet<>();
        for (ContentFolderSupportPatcher patcher : ContentFolderSupportPatcher.EP_NAME.getExtensionList()) {
            patcher.patch(moduleRootManager, providers);
        }
        providers.add(ExcludedContentFolderTypeProvider.getInstance());
        return providers;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static Image getContentFolderIcon(@Nonnull ContentFolderTypeProvider typeProvider, @Nonnull Map<Key, Object> params) {
        if (params.isEmpty()) {
            return typeProvider.getIcon();
        }

        IconDescriptor iconDescriptor = new IconDescriptor(typeProvider.getIcon());
        Application.get().getExtensionPoint(ContentFolderPropertyProvider.class).forEach(propertyProvider -> {
            Object value = propertyProvider.getKey().get(params);
            if (value == null) {
                return;
            }

            Image icon = propertyProvider.getIcon(value, typeProvider);
            if (icon == null) {
                return;
            }

            if (propertyProvider.isUpdateFullIcon()) {
                iconDescriptor.setMainIcon(icon);
            } else {
                iconDescriptor.addLayerIcon(icon);
            }
        });
        return iconDescriptor.toIcon();
    }
}

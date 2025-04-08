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
package consulo.content;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.util.dataholder.Key;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2013-11-25
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ContentFolderPropertyProvider<T> {
    public static final ExtensionPointName<ContentFolderPropertyProvider> EP_NAME =
        ExtensionPointName.create(ContentFolderPropertyProvider.class);

    @Nonnull
    public abstract Key<T> getKey();

    @Nullable
    public abstract Image getIcon(@Nonnull T value, @Nonnull ContentFolderTypeProvider typeProvider);

    public boolean isUpdateFullIcon() {
        return false;
    }

    public abstract T fromString(@Nonnull String value);

    public abstract String toString(@Nonnull T value);

    @Nonnull
    public abstract T[] getValues();

    @Nullable
    public static ContentFolderPropertyProvider<?> findProvider(@Nonnull String key) {
        for (ContentFolderPropertyProvider provider : EP_NAME.getExtensionList()) {
            if (key.equals(provider.getKey().toString())) {
                return provider;
            }
        }
        return null;
    }
}

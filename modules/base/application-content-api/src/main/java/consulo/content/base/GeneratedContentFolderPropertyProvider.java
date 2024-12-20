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
package consulo.content.base;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.content.ContentFolderPropertyProvider;
import consulo.content.ContentFolderTypeProvider;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 22:01/25.11.13
 */
@ExtensionImpl
public class GeneratedContentFolderPropertyProvider extends ContentFolderPropertyProvider<Boolean> {
    public static final Key<Boolean> IS_GENERATED = Key.create("is-generated-root");

    @Nonnull
    @Override
    public Key<Boolean> getKey() {
        return IS_GENERATED;
    }

    @Override
    public boolean isUpdateFullIcon() {
        return true;
    }


    @Nullable
    @Override
    public Image getIcon(@Nonnull Boolean value, @Nonnull ContentFolderTypeProvider typeProvider) {
        if (!Objects.equals(value, Boolean.TRUE)) {
            return null;
        }

        if (typeProvider instanceof BuiltInGeneratedIconOwner owner) {
            return owner.getGeneratedIcon();
        }

        return PlatformIconGroup.modulesGeneratedroot();
    }

    @Override
    public Boolean fromString(@Nonnull String value) {
        return Boolean.valueOf(value);
    }

    @Override
    public String toString(@Nonnull Boolean value) {
        return value.toString();
    }

    @Nonnull
    @Override
    public Boolean[] getValues() {
        return new Boolean[]{Boolean.TRUE, Boolean.FALSE};
    }
}

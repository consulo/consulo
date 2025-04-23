/*
 * Copyright 2013-2022 consulo.io
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
package consulo.content.scope;

import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileFilter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2022-02-09
 */
public interface SearchScope extends VirtualFileFilter {
    @Nonnull
    default String getDisplayName() {
        return "<unknown scope>";
    }

    @Nullable
    default Image getIcon() {
        return null;
    }

    @Nonnull
    SearchScope intersectWith(@Nonnull SearchScope scope2);

    @Nonnull
    SearchScope union(@Nonnull SearchScope scope);

    boolean contains(@Nonnull VirtualFile file);

    @Override
    default boolean accept(VirtualFile file) {
        return contains(file);
    }
}

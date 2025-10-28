/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ui.ex.keymap;

import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnAction;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * @author yole
 */
public interface KeymapGroup {
    interface Builder {
        @Nonnull
        CreatingBuilder root(@Nonnull LocalizeValue name);

        @Nonnull
        CreatingBuilder root(@Nonnull LocalizeValue name, @Nonnull Image icon);

        @Nonnull
        CreatingBuilder root(@Nonnull LocalizeValue name, @Nonnull String id, @Nonnull Image icon);
    }

    interface CreatingBuilder {
        @Nonnull
        CreatingBuilder filter(@Nullable Predicate<AnAction> filter);

        @Nonnull
        CreatingBuilder addGroup(@Nonnull String groupId);

        @Nonnull
        CreatingBuilder addGroup(@Nonnull String groupId, boolean forceNonPopup);

        @Nonnull
        CreatingBuilder addAction(AnAction action, boolean forceNonPopup);

        @Nonnull
        KeymapGroup build();
    }

    void addActionId(String id);

    void addGroup(KeymapGroup keymapGroup);

    void addAll(KeymapGroup group);

    void addSeparator();

    default void normalizeSeparators() {
    }

    int getSize();

    List<Object> getChildren();
}
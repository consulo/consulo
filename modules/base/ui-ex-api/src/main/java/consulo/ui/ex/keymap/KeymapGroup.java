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
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * @author yole
 */
public interface KeymapGroup {
    interface Builder {
        
        CreatingBuilder root(LocalizeValue name);

        
        CreatingBuilder root(LocalizeValue name, Image icon);

        
        CreatingBuilder root(LocalizeValue name, String id, Image icon);
    }

    interface CreatingBuilder {
        
        CreatingBuilder filter(@Nullable Predicate<AnAction> filter);

        
        CreatingBuilder addGroup(String groupId);

        
        CreatingBuilder addGroup(String groupId, boolean forceNonPopup);

        
        CreatingBuilder addAction(AnAction action, boolean forceNonPopup);

        
        KeymapGroup build();
    }

    void addActionId(String id);

    void addGroup(KeymapGroup keymapGroup);

    void addAll(KeymapGroup group);

    void addSeparator();

    default String getActionQualifiedPath(String id) {
        return getActionQualifiedPath(id, true);
    }

    String getActionQualifiedPath(String id, boolean presentable);

    default void normalizeSeparators() {
    }

    int getSize();

    List<Object> getChildren();
}
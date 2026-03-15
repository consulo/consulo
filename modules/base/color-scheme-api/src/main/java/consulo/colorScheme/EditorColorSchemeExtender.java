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
package consulo.colorScheme;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.ui.color.ColorValue;

/**
 * @author VISTALL
 * @since 2025-03-17
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface EditorColorSchemeExtender {
    interface Builder {
        void add(EditorColorKey key, ColorValue colorValue);

        void add(TextAttributesKey key, AttributesFlyweight attributes);

        void add(TextAttributesKey key, TextAttributesKey baseAttributes, AttributesFlyweight attributes);
    }

    void extend(Builder builder);

    
    String getColorSchemeId();
}

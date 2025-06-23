/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.colorScheme.setting;

import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.TextAttributesKey;

import jakarta.annotation.Nonnull;

/**
 * Defines interface of text/color descriptors operated by color schemes.
 *
 * @author Denis Zhdanov
 * @since 2012-01-19
 */
public interface ColorAndFontDescriptors {
    /**
     * Returns the list of descriptors specifying the {@link TextAttributesKey} instances
     * for which colors are specified in the page. For such attribute keys, the user can choose
     * all highlighting attributes (font type, background color, foreground color, error stripe color and
     * effects).
     *
     * @return the list of attribute descriptors.
     */
    @Nonnull
    AttributesDescriptor[] getAttributeDescriptors();

    /**
     * Returns the list of descriptors specifying the {@link EditorColorKey}
     * instances for which colors are specified in the page. For such color keys, the user can
     * choose only the background or foreground color.
     *
     * @return the list of color descriptors.
     */
    @Nonnull
    default ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    /**
     * Returns the title of the page, shown as text in the dialog tab.
     *
     * @return the title of the custom page.
     */
    @Nonnull
    String getDisplayName();
}

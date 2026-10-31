/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.editor.impl.internal.codeBlock;

import consulo.colorScheme.DelegateColorScheme;
import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.ui.color.ColorValue;
import consulo.ui.util.ColorValueUtil;
import org.jspecify.annotations.Nullable;

/**
 * A scheme whose colors are made more or less vivid by a factor
 *
 * @see ColorValueUtil#tuneSaturationEspeciallyGrey
 */
class SaturatedColorsScheme extends DelegateColorScheme {
    private final float mySaturationFactor;

    SaturatedColorsScheme(EditorColorsScheme delegate, float saturationFactor) {
        super(delegate.clone());
        mySaturationFactor = saturationFactor;
    }

    @Override
    public ColorValue getDefaultBackground() {
        return tuneColor(super.getDefaultBackground());
    }

    @Override
    public ColorValue getDefaultForeground() {
        return tuneColor(super.getDefaultForeground());
    }

    @Override
    public @Nullable ColorValue getColor(EditorColorKey key) {
        ColorValue color = super.getColor(key);
        return color == null ? null : tuneColor(color);
    }

    @Override
    public void setColor(EditorColorKey key, @Nullable ColorValue color) {
        super.setColor(key, color == null ? null : tuneColor(color));
    }

    @Override
    public @Nullable TextAttributes getAttributes(@Nullable TextAttributesKey key) {
        return tuneAttributes(super.getAttributes(key));
    }

    @Override
    public void setAttributes(TextAttributesKey key, TextAttributes attributes) {
        TextAttributes tuned = tuneAttributes(attributes);
        super.setAttributes(key, tuned == null ? attributes : tuned);
    }

    private ColorValue tuneColor(ColorValue color) {
        return ColorValueUtil.tuneSaturationEspeciallyGrey(color, 1, mySaturationFactor);
    }

    private @Nullable TextAttributes tuneAttributes(@Nullable TextAttributes attributes) {
        if (attributes == null) {
            return null;
        }
        TextAttributes tuned = attributes.clone();
        ColorValue foregroundColor = tuned.getForegroundColor();
        ColorValue backgroundColor = tuned.getBackgroundColor();
        if (foregroundColor != null) {
            tuned.setForegroundColor(tuneColor(foregroundColor));
        }
        if (backgroundColor != null) {
            tuned.setBackgroundColor(tuneColor(backgroundColor));
        }
        return tuned;
    }
}

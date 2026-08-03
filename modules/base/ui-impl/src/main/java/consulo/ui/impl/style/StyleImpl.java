/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui.impl.style;

import consulo.logging.Logger;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.style.StandardColors;
import consulo.ui.style.Style;
import consulo.ui.style.StyleColorValue;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 05-Nov-17
 */
public abstract class StyleImpl implements Style {
    private static final Logger LOG = Logger.getInstance(StyleImpl.class);

    public @Nullable UITheme getTheme() {
        return UITheme.get(getId());
    }

    @Override
    public String getIconLibraryId() {
        UITheme theme = getTheme();
        String iconLibraryId = theme == null ? null : theme.getIconLibraryId();
        return iconLibraryId == null ? Style.super.getIconLibraryId() : iconLibraryId;
    }

    @Override
    public ColorValue getColorValue(StyleColorValue colorValue) {
        if (colorValue instanceof StandardColors standardColors) {
            return standardColors.getStaticValue();
        }

        String key = StyleColorKeys.getKey(colorValue);
        if (key != null) {
            UITheme theme = getTheme();
            if (theme != null) {
                RGBColor color = theme.getColor(key);
                if (color != null) {
                    return color;
                }
            }
        }

        return getUnresolvedColorValue(colorValue);
    }

    protected ColorValue getUnresolvedColorValue(StyleColorValue colorValue) {
        LOG.warn("Unresolved color: " + colorValue + ", key: " + StyleColorKeys.getKey(colorValue) + ", style: " + getId());
        return StandardColors.RED.getStaticValue();
    }
}

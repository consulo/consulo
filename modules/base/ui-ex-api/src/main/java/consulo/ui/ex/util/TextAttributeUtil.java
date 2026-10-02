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
package consulo.ui.ex.util;

import consulo.colorScheme.EffectType;
import consulo.colorScheme.TextAttributes;
import consulo.ui.TextAttribute;
import consulo.ui.TextEffect;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.font.Font;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class TextAttributeUtil {
    private static final int FONT_STYLE_MASK = Font.BOLD | Font.ITALIC;

    private TextAttributeUtil() {
    }

    public static TextAttribute toTextAttribute(@Nullable SimpleTextAttributes attributes) {
        return toTextAttribute(attributes, null);
    }

    public static TextAttribute toTextAttribute(@Nullable SimpleTextAttributes attributes, @Nullable ColorValue defaultForeground) {
        if (attributes == null) {
            return defaultForeground == null ? TextAttribute.REGULAR : new TextAttribute(Font.PLAIN, defaultForeground);
        }

        ColorValue foreground = attributes.foreground();
        TextAttribute attribute = new TextAttribute(
            attributes.getStyle() & FONT_STYLE_MASK,
            foreground == null ? defaultForeground : foreground,
            attributes.background()
        );

        if (attributes.isStrikeout()) {
            attribute = attribute.withEffect(TextEffect.STRIKEOUT);
        }
        if (attributes.isUnderline() || attributes.isBoldDottedLine()) {
            attribute = attribute.withEffect(TextEffect.UNDERLINE);
        }
        if (attributes.isWaved()) {
            attribute = attribute.withEffect(TextEffect.WAVED);
        }
        return attribute;
    }

    public static TextAttribute toTextAttribute(@Nullable TextAttributes attributes) {
        if (attributes == null) {
            return TextAttribute.REGULAR;
        }

        Set<TextEffect> effects = EnumSet.noneOf(TextEffect.class);
        attributes.forEachEffect((effectType, effectColor) -> {
            TextEffect effect = effectColor == null ? null : toTextEffect(effectType);
            if (effect != null) {
                effects.add(effect);
            }
        });

        TextAttribute attribute = new TextAttribute(
            attributes.getFontType() & FONT_STYLE_MASK,
            attributes.getForegroundColor(),
            attributes.getBackgroundColor()
        );
        for (TextEffect effect : effects) {
            attribute = attribute.withEffect(effect);
        }
        return attribute;
    }

    public static SimpleTextAttributes toSimpleTextAttributes(TextAttribute attribute) {
        int style = attribute.getStyle();
        for (TextEffect effect : attribute.getEffects()) {
            style |= switch (effect) {
                case STRIKEOUT -> SimpleTextAttributes.STYLE_STRIKEOUT;
                case UNDERLINE -> SimpleTextAttributes.STYLE_UNDERLINE;
                case WAVED -> SimpleTextAttributes.STYLE_WAVED;
            };
        }

        return SimpleTextAttributes.of(attribute.getBackgroundColor(), attribute.getForegroundColor(), null, style);
    }

    public static Set<TextEffect> getEffects(TextAttribute attribute) {
        int style = attribute.getStyle();
        if ((style & ~FONT_STYLE_MASK) == 0) {
            return attribute.getEffects();
        }

        Set<TextEffect> effects = EnumSet.noneOf(TextEffect.class);
        effects.addAll(attribute.getEffects());
        if ((style & SimpleTextAttributes.STYLE_STRIKEOUT) != 0) {
            effects.add(TextEffect.STRIKEOUT);
        }
        if ((style & (SimpleTextAttributes.STYLE_UNDERLINE | SimpleTextAttributes.STYLE_BOLD_DOTTED_LINE)) != 0) {
            effects.add(TextEffect.UNDERLINE);
        }
        if ((style & SimpleTextAttributes.STYLE_WAVED) != 0) {
            effects.add(TextEffect.WAVED);
        }
        return Collections.unmodifiableSet(effects);
    }

    public static int getFontStyle(TextAttribute attribute) {
        return attribute.getStyle() & FONT_STYLE_MASK;
    }

    private static @Nullable TextEffect toTextEffect(@Nullable EffectType effectType) {
        if (effectType == null) {
            return null;
        }

        return switch (effectType) {
            case STRIKEOUT -> TextEffect.STRIKEOUT;
            case WAVE_UNDERSCORE -> TextEffect.WAVED;
            case LINE_UNDERSCORE, BOLD_LINE_UNDERSCORE, BOLD_DOTTED_LINE -> TextEffect.UNDERLINE;
            default -> null;
        };
    }
}

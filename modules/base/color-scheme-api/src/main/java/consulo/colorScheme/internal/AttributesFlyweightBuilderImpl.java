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
package consulo.colorScheme.internal;

import consulo.colorScheme.AttributesFlyweight;
import consulo.colorScheme.AttributesFlyweightBuilder;
import consulo.colorScheme.EffectType;
import consulo.ui.color.ColorValue;
import consulo.util.lang.BitUtil;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2025-03-17
 */
public class AttributesFlyweightBuilderImpl implements AttributesFlyweightBuilder {
    private @Nullable ColorValue myForeground;
    private @Nullable ColorValue myBackground;
    private int myFontType;
    private @Nullable ColorValue myEffectColor;
    private @Nullable EffectType myEffectType;
    private Map<EffectType, ColorValue> myAdditionalEffects = Map.of();
    private @Nullable ColorValue myErrorStripeColor;

    @Override
    public AttributesFlyweightBuilder withForeground(ColorValue foreground) {
        myForeground = foreground;
        return this;
    }

    @Override
    public AttributesFlyweightBuilder withBackground(ColorValue background) {
        myBackground = background;
        return this;
    }

    @Override
    public AttributesFlyweightBuilder withEffect(EffectType effectType, @Nullable ColorValue effectColor) {
        myEffectColor = effectColor;
        myEffectType = effectType;
        return this;
    }

    @Override
    public AttributesFlyweightBuilder withAdditionalEffect(EffectType effectType, @Nullable ColorValue effectColor) {
        if (myAdditionalEffects.isEmpty()) {
            myAdditionalEffects = new LinkedHashMap<>();
        }

        myAdditionalEffects.put(effectType, effectColor);
        return this;
    }

    @Override
    public AttributesFlyweightBuilder withErrorStripeColor(ColorValue errorStripeColor) {
        myErrorStripeColor = errorStripeColor;
        return this;
    }

    @Override
    public AttributesFlyweightBuilder withBoldFont() {
        myFontType = BitUtil.set(myFontType, Font.BOLD, true);
        return this;
    }

    @Override
    public AttributesFlyweightBuilder withItalicFont() {
        myFontType = BitUtil.set(myFontType, Font.ITALIC, true);
        return this;
    }

    @Override
    public AttributesFlyweight build() {
        return AttributesFlyweight.create(myForeground,
            myBackground,
            myFontType,
            myEffectColor,
            myEffectType,
            myAdditionalEffects,
            myErrorStripeColor
        );
    }
}

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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2025-03-17
 */
public class AttributesFlyweightBuilderImpl implements AttributesFlyweightBuilder {
    private ColorValue myForeground;
    private ColorValue myBackground;
    private int myFontType;
    private ColorValue myEffectColor;
    private EffectType myEffectType;
    private Map<EffectType, ColorValue> myAdditionalEffects = Map.of();
    private ColorValue myErrorStripeColor;

    @Nonnull
    @Override
    public AttributesFlyweightBuilder withForeground(@Nonnull ColorValue foreground) {
        myForeground = foreground;
        return this;
    }

    @Nonnull
    @Override
    public AttributesFlyweightBuilder withBackground(@Nonnull ColorValue background) {
        myBackground = background;
        return this;
    }

    @Nonnull
    @Override
    public AttributesFlyweightBuilder withEffect(@Nonnull EffectType effectType, @Nullable ColorValue effectColor) {
        myEffectColor = effectColor;
        myEffectType = effectType;
        return this;
    }

    @Nonnull
    @Override
    public AttributesFlyweightBuilder withAdditionalEffect(@Nonnull EffectType effectType, @Nullable ColorValue effectColor) {
        if (myAdditionalEffects.isEmpty()) {
            myAdditionalEffects = new LinkedHashMap<>();
        }

        myAdditionalEffects.put(effectType, effectColor);
        return this;
    }

    @Nonnull
    @Override
    public AttributesFlyweightBuilder withErrorStripeColor(@Nonnull ColorValue errorStripeColor) {
        myErrorStripeColor = errorStripeColor;
        return this;
    }

    @Nonnull
    @Override
    public AttributesFlyweightBuilder withBoldFont() {
        myFontType = BitUtil.set(myFontType, Font.BOLD, true);
        return this;
    }

    @Nonnull
    @Override
    public AttributesFlyweightBuilder withItalicFont() {
        myFontType = BitUtil.set(myFontType, Font.ITALIC, true);
        return this;
    }

    @Nonnull
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

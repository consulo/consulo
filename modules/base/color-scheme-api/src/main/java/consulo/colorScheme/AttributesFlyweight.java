/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.logging.Logger;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.util.collection.Maps;
import consulo.util.xml.serializer.DefaultJDOMExternalizer;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.JDOMExternalizerUtil;
import org.intellij.lang.annotations.JdkConstants;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author max
 */
public class AttributesFlyweight {
    private static final Logger LOG = Logger.getInstance(AttributesFlyweight.class);

    private static final ConcurrentMap<FlyweightKey, AttributesFlyweight> entries = new ConcurrentHashMap<>();
    private static final ThreadLocal<FlyweightKey> ourKey = new ThreadLocal<>();

    private final int myHashCode;
    private final @Nullable ColorValue myForeground;
    private final @Nullable ColorValue myBackground;
    @JdkConstants.FontStyle
    private final int myFontType;
    private final @Nullable ColorValue myEffectColor;
    private final @Nullable EffectType myEffectType;
    
    private final Map<EffectType, ColorValue> myAdditionalEffects;
    private final @Nullable ColorValue myErrorStripeColor;

    private static class FlyweightKey implements Cloneable {
        private @Nullable ColorValue foreground = null;
        private @Nullable ColorValue background = null;
        @JdkConstants.FontStyle
        private int fontType;
        private @Nullable ColorValue effectColor = null;
        private @Nullable EffectType effectType = null;
        private @Nullable ColorValue errorStripeColor = null;
        
        private Map<EffectType, ColorValue> myAdditionalEffects = Collections.emptyMap();

        private FlyweightKey() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof FlyweightKey)) {
                return false;
            }

            FlyweightKey key = (FlyweightKey) o;

            if (fontType != key.fontType) {
                return false;
            }
            if (!Objects.equals(background, key.background)) {
                return false;
            }
            if (!Objects.equals(effectColor, key.effectColor)) {
                return false;
            }
            if (effectType != key.effectType) {
                return false;
            }
            if (!Objects.equals(errorStripeColor, key.errorStripeColor)) {
                return false;
            }
            if (!Objects.equals(foreground, key.foreground)) {
                return false;
            }
            if (!myAdditionalEffects.equals(key.myAdditionalEffects)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(foreground);
            result = 31 * result + Objects.hashCode(background);
            result = 31 * result + fontType;
            result = 31 * result + Objects.hashCode(effectColor);
            result = 31 * result + Objects.hashCode(effectType);
            result = 31 * result + Objects.hashCode(errorStripeColor);
            result = 31 * result + myAdditionalEffects.hashCode();
            return result;
        }

        @Override
        protected FlyweightKey clone() {
            try {
                return (FlyweightKey) super.clone();
            }
            catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static AttributesFlyweight create(
        @Nullable ColorValue foreground,
        @Nullable ColorValue background,
        @JdkConstants.FontStyle int fontType,
        @Nullable ColorValue effectColor,
        @Nullable EffectType effectType,
        @Nullable ColorValue errorStripeColor
    ) {
        return create(foreground, background, fontType, effectColor, effectType, Collections.emptyMap(), errorStripeColor);
    }

    public static AttributesFlyweight create(
        @Nullable ColorValue foreground,
        @Nullable ColorValue background,
        @JdkConstants.FontStyle int fontType,
        @Nullable ColorValue effectColor,
        @Nullable EffectType effectType,
        Map<EffectType, ColorValue> additionalEffects,
        @Nullable ColorValue errorStripeColor
    ) {
        FlyweightKey key = createKey(foreground, background, fontType, effectColor, effectType, additionalEffects, errorStripeColor);

        AttributesFlyweight flyweight = entries.get(key);
        if (flyweight != null) {
            return flyweight;
        }

        return Maps.cacheOrGet(entries, key.clone(), new AttributesFlyweight(key));
    }

    public static AttributesFlyweight createNoCache(
        @Nullable ColorValue foreground,
        @Nullable ColorValue background,
        @JdkConstants.FontStyle int fontType,
        @Nullable ColorValue effectColor,
        @Nullable EffectType effectType,
        Map<EffectType, ? extends ColorValue> additionalEffects,
        @Nullable ColorValue errorStripeColor
    ) {
        FlyweightKey key = createKey(foreground, background, fontType, effectColor, effectType, additionalEffects, errorStripeColor);
        return new AttributesFlyweight(key);
    }

    private static FlyweightKey createKey(
        @Nullable ColorValue foreground,
        @Nullable ColorValue background,
        @JdkConstants.FontStyle int fontType,
        @Nullable ColorValue effectColor,
        @Nullable EffectType effectType,
        Map<EffectType, ? extends ColorValue> additionalEffects,
        @Nullable ColorValue errorStripeColor
    ) {
        FlyweightKey key = ourKey.get();
        if (key == null) {
            ourKey.set(key = new FlyweightKey());
        }
        key.foreground = foreground;
        key.background = background;
        key.fontType = fontType;
        key.effectColor = effectColor;
        key.effectType = effectType;
        key.myAdditionalEffects = additionalEffects.isEmpty() ? Collections.emptyMap() : new EnumMap<>(additionalEffects);
        key.errorStripeColor = errorStripeColor;
        return key;
    }

    private AttributesFlyweight(FlyweightKey key) {
        myForeground = key.foreground;
        myBackground = key.background;
        myFontType = key.fontType;
        myEffectColor = key.effectColor;
        myEffectType = key.effectType;
        myErrorStripeColor = key.errorStripeColor;
        myAdditionalEffects = key.myAdditionalEffects;
        myHashCode = key.hashCode();
    }

    public static AttributesFlyweight create(Element element) throws InvalidDataException {
        ColorValue FOREGROUND = toColor(JDOMExternalizerUtil.readField(element, "FOREGROUND"));
        ColorValue BACKGROUND = toColor(JDOMExternalizerUtil.readField(element, "BACKGROUND"));
        ColorValue EFFECT_COLOR = toColor(JDOMExternalizerUtil.readField(element, "EFFECT_COLOR"));
        ColorValue ERROR_STRIPE_COLOR = toColor(JDOMExternalizerUtil.readField(element, "ERROR_STRIPE_COLOR"));
        int fontType = DefaultJDOMExternalizer.toInt(JDOMExternalizerUtil.readField(element, "FONT_TYPE", "0"));
        if (fontType < 0 || fontType > 3) {
            fontType = 0;
        }
        int FONT_TYPE = fontType;
        int EFFECT_TYPE = DefaultJDOMExternalizer.toInt(JDOMExternalizerUtil.readField(element, "EFFECT_TYPE", "0"));
        // todo additionalEffects are not serialized yet, we have no user-controlled additional effects
        return create(
            FOREGROUND,
            BACKGROUND,
            FONT_TYPE,
            EFFECT_COLOR,
            toEffectType(EFFECT_TYPE),
            Collections.emptyMap(),
            ERROR_STRIPE_COLOR
        );
    }

    public static @Nullable ColorValue toColor(@Nullable String value) throws InvalidDataException {
        ColorValue color;
        if (value == null) {
            color = null;
        }
        else {
            try {
                int rgb = Integer.parseInt(value, 16);
                color = RGBColor.fromRGBValue(rgb);
            }
            catch (NumberFormatException ex) {
                LOG.debug("Wrong color value: " + value, ex);
                throw new InvalidDataException("Wrong color value: " + value, ex);
            }
        }
        return color;
    }

    private static void writeColor(Element element, String fieldName, @Nullable ColorValue color) {
        if (color != null) {
            int rgb = RGBColor.toRGBValue(color.toRGB());
            String string = Integer.toString(rgb & 0xFFFFFF, 16);
            JDOMExternalizerUtil.writeField(element, fieldName, string);
        }
    }

    void writeExternal(Element element) {
        writeColor(element, "FOREGROUND", getForeground());
        writeColor(element, "BACKGROUND", getBackground());
        int fontType = getFontType();
        if (fontType != 0) {
            JDOMExternalizerUtil.writeField(element, "FONT_TYPE", String.valueOf(fontType));
        }
        writeColor(element, "EFFECT_COLOR", getEffectColor());
        writeColor(element, "ERROR_STRIPE_COLOR", getErrorStripeColor());
        int effectType = fromEffectType(getEffectType());
        if (effectType != 0) {
            JDOMExternalizerUtil.writeField(element, "EFFECT_TYPE", String.valueOf(effectType));
        }
        // todo additionalEffects are not serialized yet, we have no user-controlled additional effects
    }

    private static final int EFFECT_BORDER = 0;
    private static final int EFFECT_LINE = 1;
    private static final int EFFECT_WAVE = 2;
    private static final int EFFECT_STRIKEOUT = 3;
    private static final int EFFECT_BOLD_LINE = 4;
    private static final int EFFECT_BOLD_DOTTED_LINE = 5;

    private static int fromEffectType(@Nullable EffectType effectType) {
        if (effectType == null) {
            return -1;
        }
        return switch (effectType) {
            case BOXED -> EFFECT_BORDER;
            case LINE_UNDERSCORE -> EFFECT_LINE;
            case BOLD_LINE_UNDERSCORE -> EFFECT_BOLD_LINE;
            case STRIKEOUT -> EFFECT_STRIKEOUT;
            case WAVE_UNDERSCORE -> EFFECT_WAVE;
            case BOLD_DOTTED_LINE -> EFFECT_BOLD_DOTTED_LINE;
            default -> -1;
        };
    }

    private static @Nullable EffectType toEffectType(int effectType) {
        return switch (effectType) {
            case EFFECT_BORDER -> EffectType.BOXED;
            case EFFECT_BOLD_LINE -> EffectType.BOLD_LINE_UNDERSCORE;
            case EFFECT_LINE -> EffectType.LINE_UNDERSCORE;
            case EFFECT_STRIKEOUT -> EffectType.STRIKEOUT;
            case EFFECT_WAVE -> EffectType.WAVE_UNDERSCORE;
            case EFFECT_BOLD_DOTTED_LINE -> EffectType.BOLD_DOTTED_LINE;
            default -> null;
        };
    }

    public @Nullable ColorValue getForeground() {
        return myForeground;
    }

    public @Nullable ColorValue getBackground() {
        return myBackground;
    }

    @JdkConstants.FontStyle
    public int getFontType() {
        return myFontType;
    }

    public @Nullable ColorValue getEffectColor() {
        return myEffectColor;
    }

    public @Nullable EffectType getEffectType() {
        return myEffectType;
    }

    Map<EffectType, ColorValue> getAdditionalEffects() {
        return myAdditionalEffects;
    }

    /**
     * @return true iff there are effects to draw in this attributes
     */
    public boolean hasEffects() {
        return myEffectColor != null && myEffectType != null || !myAdditionalEffects.isEmpty();
    }

    /**
     * @return all attributes effects, main and additional ones
     */
    Map<EffectType, ColorValue> getAllEffects() {
        if (myAdditionalEffects.isEmpty()) {
            return myEffectType == null || myEffectColor == null ? Collections.emptyMap() : Collections.singletonMap(
                myEffectType,
                myEffectColor
            );
        }
        TextAttributesEffectsBuilder builder = TextAttributesEffectsBuilder.create();
        myAdditionalEffects.forEach(builder::coverWith);
        builder.coverWith(myEffectType, myEffectColor);
        return builder.getEffectsMap();
    }

    public @Nullable ColorValue getErrorStripeColor() {
        return myErrorStripeColor;
    }

    public AttributesFlyweight withForeground(@Nullable ColorValue foreground) {
        return Objects.equals(foreground, myForeground) ? this : create(
            foreground,
            myBackground,
            myFontType,
            myEffectColor,
            myEffectType,
            myAdditionalEffects,
            myErrorStripeColor
        );
    }

    public AttributesFlyweight withBackground(@Nullable ColorValue background) {
        return Objects.equals(background, myBackground) ? this : create(
            myForeground,
            background,
            myFontType,
            myEffectColor,
            myEffectType,
            myAdditionalEffects,
            myErrorStripeColor
        );
    }

    public AttributesFlyweight withFontType(@JdkConstants.FontStyle int fontType) {
        return fontType == myFontType ? this : create(
            myForeground,
            myBackground,
            fontType,
            myEffectColor,
            myEffectType,
            myAdditionalEffects,
            myErrorStripeColor
        );
    }

    public AttributesFlyweight withEffectColor(@Nullable ColorValue effectColor) {
        return Objects.equals(effectColor, myEffectColor) ? this : create(
            myForeground,
            myBackground,
            myFontType,
            effectColor,
            myEffectType,
            myAdditionalEffects,
            myErrorStripeColor
        );
    }

    public AttributesFlyweight withEffectType(@Nullable EffectType effectType) {
        return Objects.equals(effectType, myEffectType) ? this : create(
            myForeground,
            myBackground,
            myFontType,
            myEffectColor,
            effectType,
            myAdditionalEffects,
            myErrorStripeColor
        );
    }

    public AttributesFlyweight withErrorStripeColor(@Nullable ColorValue stripeColor) {
        return Objects.equals(stripeColor, myErrorStripeColor) ? this : create(
            myForeground,
            myBackground,
            myFontType,
            myEffectColor,
            myEffectType,
            myAdditionalEffects,
            stripeColor
        );
    }

    /**
     * @see TextAttributes#setAdditionalEffects(java.util.Map)
     */
    //@ApiStatus.Experimental
    public AttributesFlyweight withAdditionalEffects(Map<EffectType, ColorValue> additionalEffects) {
        return Objects.equals(additionalEffects, myAdditionalEffects) ? this : create(
            myForeground,
            myBackground,
            myFontType,
            myEffectColor,
            myEffectType,
            additionalEffects,
            myErrorStripeColor
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AttributesFlyweight that = (AttributesFlyweight) o;

        if (myFontType != that.myFontType) {
            return false;
        }
        if (!Objects.equals(myBackground, that.myBackground)) {
            return false;
        }
        if (!Objects.equals(myEffectColor, that.myEffectColor)) {
            return false;
        }
        if (myEffectType != that.myEffectType) {
            return false;
        }
        if (!Objects.equals(myErrorStripeColor, that.myErrorStripeColor)) {
            return false;
        }
        if (!Objects.equals(myForeground, that.myForeground)) {
            return false;
        }
        if (!myAdditionalEffects.equals(that.myAdditionalEffects)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return myHashCode;
    }

    @Override
    public String toString() {
        return "AttributesFlyweight{myForeground=" + myForeground +
            ", myBackground=" + myBackground +
            ", myFontType=" + myFontType +
            ", myEffectColor=" + myEffectColor +
            ", myEffectType=" + myEffectType +
            ", myErrorStripeColor=" + myErrorStripeColor +
            '}';
    }
}

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

/*
 * @author max
 */
package com.intellij.openapi.editor.markup;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.util.ConcurrencyUtil;
import consulo.logging.Logger;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import org.intellij.lang.annotations.JdkConstants;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AttributesFlyweight {
  private static final Logger LOG = Logger.getInstance(AttributesFlyweight.class);
  
  private static final ConcurrentMap<FlyweightKey, AttributesFlyweight> entries = new ConcurrentHashMap<>();
  private static final ThreadLocal<FlyweightKey> ourKey = new ThreadLocal<>();

  private final int myHashCode;
  private final ColorValue myForeground;
  private final ColorValue myBackground;
  @JdkConstants.FontStyle
  private final int myFontType;
  private final ColorValue myEffectColor;
  private final EffectType myEffectType;
  @Nonnull
  private final Map<EffectType, ColorValue> myAdditionalEffects;
  private final ColorValue myErrorStripeColor;

  private static class FlyweightKey implements Cloneable {
    private ColorValue foreground;
    private ColorValue background;
    @JdkConstants.FontStyle
    private int fontType;
    private ColorValue effectColor;
    private EffectType effectType;
    private ColorValue errorStripeColor;
    @Nonnull
    private Map<EffectType, ColorValue> myAdditionalEffects = Collections.emptyMap();

    private FlyweightKey() {
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof FlyweightKey)) return false;

      FlyweightKey key = (FlyweightKey)o;

      if (fontType != key.fontType) return false;
      if (background != null ? !background.equals(key.background) : key.background != null) return false;
      if (effectColor != null ? !effectColor.equals(key.effectColor) : key.effectColor != null) return false;
      if (effectType != key.effectType) return false;
      if (errorStripeColor != null ? !errorStripeColor.equals(key.errorStripeColor) : key.errorStripeColor != null) return false;
      if (foreground != null ? !foreground.equals(key.foreground) : key.foreground != null) return false;
      if (!myAdditionalEffects.equals(key.myAdditionalEffects)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = foreground != null ? foreground.hashCode() : 0;
      result = 31 * result + (background != null ? background.hashCode() : 0);
      result = 31 * result + fontType;
      result = 31 * result + (effectColor != null ? effectColor.hashCode() : 0);
      result = 31 * result + (effectType != null ? effectType.hashCode() : 0);
      result = 31 * result + (errorStripeColor != null ? errorStripeColor.hashCode() : 0);
      result = 31 * result + myAdditionalEffects.hashCode();
      return result;
    }

    @Override
    protected FlyweightKey clone() {
      try {
        return (FlyweightKey)super.clone();
      }
      catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Nonnull
  public static AttributesFlyweight create(ColorValue foreground,
                                           ColorValue background,
                                           @JdkConstants.FontStyle int fontType,
                                           ColorValue effectColor,
                                           EffectType effectType,
                                           ColorValue errorStripeColor) {
    return create(foreground, background, fontType, effectColor, effectType, Collections.emptyMap(), errorStripeColor);
  }

  //@ApiStatus.Experimental
  @Nonnull
  public static AttributesFlyweight create(ColorValue foreground,
                                           ColorValue background,
                                           @JdkConstants.FontStyle int fontType,
                                           ColorValue effectColor,
                                           EffectType effectType,
                                           @Nonnull Map<EffectType, ColorValue> additionalEffects,
                                           ColorValue errorStripeColor) {
    FlyweightKey key = ourKey.get();
    if (key == null) {
      ourKey.set(key = new FlyweightKey());
    }
    key.foreground = foreground;
    key.background = background;
    key.fontType = fontType;
    key.effectColor = effectColor;
    key.effectType = effectType;
    key.myAdditionalEffects = additionalEffects.isEmpty() ? Collections.emptyMap() : new HashMap<>(additionalEffects);
    key.errorStripeColor = errorStripeColor;

    AttributesFlyweight flyweight = entries.get(key);
    if (flyweight != null) {
      return flyweight;
    }

    return ConcurrencyUtil.cacheOrGet(entries, key.clone(), new AttributesFlyweight(key));
  }

  private AttributesFlyweight(@Nonnull FlyweightKey key) {
    myForeground = key.foreground;
    myBackground = key.background;
    myFontType = key.fontType;
    myEffectColor = key.effectColor;
    myEffectType = key.effectType;
    myErrorStripeColor = key.errorStripeColor;
    myAdditionalEffects = key.myAdditionalEffects;
    myHashCode = key.hashCode();
  }

  @Nonnull
  public static AttributesFlyweight create(@Nonnull Element element) throws InvalidDataException {
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
    return create(FOREGROUND, BACKGROUND, FONT_TYPE, EFFECT_COLOR, toEffectType(EFFECT_TYPE), Collections.emptyMap(), ERROR_STRIPE_COLOR);
  }

  public static ColorValue toColor(@Nullable String value) throws InvalidDataException {
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

  private static void writeColor(Element element, String fieldName, ColorValue color) {
    if (color != null) {
      int rgb = RGBColor.toRGBValue(color.toRGB());
      String string = Integer.toString(rgb & 0xFFFFFF, 16);
      JDOMExternalizerUtil.writeField(element, fieldName, string);
    }
  }

  void writeExternal(@Nonnull Element element) {
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

  private static int fromEffectType(EffectType effectType) {
    if (effectType == null) return -1;
    switch (effectType) {
      case BOXED:
        return EFFECT_BORDER;
      case LINE_UNDERSCORE:
        return EFFECT_LINE;
      case BOLD_LINE_UNDERSCORE:
        return EFFECT_BOLD_LINE;
      case STRIKEOUT:
        return EFFECT_STRIKEOUT;
      case WAVE_UNDERSCORE:
        return EFFECT_WAVE;
      case BOLD_DOTTED_LINE:
        return EFFECT_BOLD_DOTTED_LINE;
      default:
        return -1;
    }
  }

  private static EffectType toEffectType(int effectType) {
    switch (effectType) {
      case EFFECT_BORDER:
        return EffectType.BOXED;
      case EFFECT_BOLD_LINE:
        return EffectType.BOLD_LINE_UNDERSCORE;
      case EFFECT_LINE:
        return EffectType.LINE_UNDERSCORE;
      case EFFECT_STRIKEOUT:
        return EffectType.STRIKEOUT;
      case EFFECT_WAVE:
        return EffectType.WAVE_UNDERSCORE;
      case EFFECT_BOLD_DOTTED_LINE:
        return EffectType.BOLD_DOTTED_LINE;
      default:
        return null;
    }
  }

  public ColorValue getForeground() {
    return myForeground;
  }

  public ColorValue getBackground() {
    return myBackground;
  }

  @JdkConstants.FontStyle
  public int getFontType() {
    return myFontType;
  }

  public ColorValue getEffectColor() {
    return myEffectColor;
  }

  public EffectType getEffectType() {
    return myEffectType;
  }

  @Nonnull
  Map<EffectType, ColorValue> getAdditionalEffects() {
    return myAdditionalEffects;
  }

  /**
   * @return true iff there are effects to draw in this attributes
   */
  //@ApiStatus.Experimental
  public boolean hasEffects() {
    return myEffectColor != null && myEffectType != null || !myAdditionalEffects.isEmpty();
  }

  /**
   * @return all attributes effects, main and additional ones
   */
  @Nonnull
  Map<EffectType, ColorValue> getAllEffects() {
    if (myAdditionalEffects.isEmpty()) {
      return myEffectType == null || myEffectColor == null ? Collections.emptyMap() : Collections.singletonMap(myEffectType, myEffectColor);
    }
    TextAttributesEffectsBuilder builder = TextAttributesEffectsBuilder.create();
    myAdditionalEffects.forEach(builder::coverWith);
    builder.coverWith(myEffectType, myEffectColor);
    return builder.getEffectsMap();
  }

  public ColorValue getErrorStripeColor() {
    return myErrorStripeColor;
  }

  @Nonnull
  public AttributesFlyweight withForeground(ColorValue foreground) {
    return Comparing.equal(foreground, myForeground) ? this : create(foreground, myBackground, myFontType, myEffectColor, myEffectType, myAdditionalEffects, myErrorStripeColor);
  }

  @Nonnull
  public AttributesFlyweight withBackground(ColorValue background) {
    return Comparing.equal(background, myBackground) ? this : create(myForeground, background, myFontType, myEffectColor, myEffectType, myAdditionalEffects, myErrorStripeColor);
  }

  @Nonnull
  public AttributesFlyweight withFontType(@JdkConstants.FontStyle int fontType) {
    return fontType == myFontType ? this : create(myForeground, myBackground, fontType, myEffectColor, myEffectType, myAdditionalEffects, myErrorStripeColor);
  }

  @Nonnull
  public AttributesFlyweight withEffectColor(ColorValue effectColor) {
    return Comparing.equal(effectColor, myEffectColor) ? this : create(myForeground, myBackground, myFontType, effectColor, myEffectType, myAdditionalEffects, myErrorStripeColor);
  }

  @Nonnull
  public AttributesFlyweight withEffectType(EffectType effectType) {
    return Comparing.equal(effectType, myEffectType) ? this : create(myForeground, myBackground, myFontType, myEffectColor, effectType, myAdditionalEffects, myErrorStripeColor);
  }

  @Nonnull
  public AttributesFlyweight withErrorStripeColor(ColorValue stripeColor) {
    return Comparing.equal(stripeColor, myErrorStripeColor) ? this : create(myForeground, myBackground, myFontType, myEffectColor, myEffectType, myAdditionalEffects, stripeColor);
  }

  /**
   * @see TextAttributes#setAdditionalEffects(java.util.Map)
   */
  @Nonnull
  //@ApiStatus.Experimental
  public AttributesFlyweight withAdditionalEffects(@Nonnull Map<EffectType, ColorValue> additionalEffects) {
    return Comparing.equal(additionalEffects, myAdditionalEffects) ? this : create(myForeground, myBackground, myFontType, myEffectColor, myEffectType, additionalEffects, myErrorStripeColor);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AttributesFlyweight that = (AttributesFlyweight)o;

    if (myFontType != that.myFontType) return false;
    if (myBackground != null ? !myBackground.equals(that.myBackground) : that.myBackground != null) return false;
    if (myEffectColor != null ? !myEffectColor.equals(that.myEffectColor) : that.myEffectColor != null) return false;
    if (myEffectType != that.myEffectType) return false;
    if (myErrorStripeColor != null ? !myErrorStripeColor.equals(that.myErrorStripeColor) : that.myErrorStripeColor != null) return false;
    if (myForeground != null ? !myForeground.equals(that.myForeground) : that.myForeground != null) return false;
    if (!myAdditionalEffects.equals(that.myAdditionalEffects)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myHashCode;
  }

  @NonNls
  @Override
  public String toString() {
    return "AttributesFlyweight{myForeground=" +
           myForeground +
           ", myBackground=" +
           myBackground +
           ", myFontType=" +
           myFontType +
           ", myEffectColor=" +
           myEffectColor +
           ", myEffectType=" +
           myEffectType +
           ", myErrorStripeColor=" +
           myErrorStripeColor +
           '}';
  }
}

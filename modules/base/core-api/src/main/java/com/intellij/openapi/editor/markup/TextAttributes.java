/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.editor.markup;

import consulo.logging.Logger;
import consulo.ui.color.ColorValue;
import org.intellij.lang.annotations.JdkConstants;
import org.jdom.Element;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Defines the visual representation (colors and effects) of text.
 */
public class TextAttributes implements Cloneable {
  private static final Logger LOG = Logger.getInstance(TextAttributes.class);
  private static final AttributesFlyweight DEFAULT_FLYWEIGHT = AttributesFlyweight.create(null, null, Font.PLAIN, null, EffectType.BOXED, Collections.emptyMap(), null);

  public static final TextAttributes ERASE_MARKER = new TextAttributes();

  private boolean myEnforceEmpty;

  @SuppressWarnings({"NullableProblems", "NotNullFieldNotInitialized"})
  @Nonnull
  private AttributesFlyweight myAttrs;

  /**
   * Merges (layers) the two given text attributes.
   *
   * @param under Text attributes to merge "under".
   * @param above Text attributes to merge "above", overriding settings from "under".
   * @return Merged attributes instance.
   */
  @Contract("!null, !null -> !null")
  public static TextAttributes merge(@Nullable TextAttributes under, @Nullable TextAttributes above) {
    if (under == null) return above;
    if (above == null) return under;

    TextAttributes attrs = under.clone();
    if (above.getBackgroundColor() != null) {
      attrs.setBackgroundColor(above.getBackgroundColor());
    }
    if (above.getForegroundColor() != null) {
      attrs.setForegroundColor(above.getForegroundColor());
    }
    attrs.setFontType(above.getFontType() | under.getFontType());

    TextAttributesEffectsBuilder.create(under).coverWith(above).applyTo(attrs);

    return attrs;
  }

  public TextAttributes() {
    this(DEFAULT_FLYWEIGHT);
  }

  private TextAttributes(@Nonnull AttributesFlyweight attributesFlyweight) {
    myAttrs = attributesFlyweight;
  }

  public TextAttributes(@Nonnull Element element) {
    readExternal(element);
  }

  private TextAttributes(@Nonnull AttributesFlyweight attributesFlyweight, boolean enforced) {
    myAttrs = attributesFlyweight;
    myEnforceEmpty = enforced;
  }

  public TextAttributes(@Nullable ColorValue foregroundColor, @Nullable ColorValue backgroundColor, @Nullable ColorValue effectColor, EffectType effectType, @JdkConstants.FontStyle int fontType) {
    setAttributes(foregroundColor, backgroundColor, effectColor, null, effectType, fontType);
  }

  public void copyFrom(@Nonnull TextAttributes other) {
    myAttrs = other.myAttrs;
  }

  public void setAttributes(ColorValue foregroundColor, ColorValue backgroundColor, ColorValue effectColor, ColorValue errorStripeColor, EffectType effectType, @JdkConstants.FontStyle int fontType) {
    setAttributes(foregroundColor, backgroundColor, effectColor, errorStripeColor, effectType, Collections.emptyMap(), fontType);
  }

  //@ApiStatus.Experimental
  public void setAttributes(ColorValue foregroundColor,
                            ColorValue backgroundColor,
                            ColorValue effectColor,
                            ColorValue errorStripeColor,
                            EffectType effectType,
                            @Nonnull Map<EffectType, ColorValue> additionalEffects,
                            @JdkConstants.FontStyle int fontType) {
    myAttrs = AttributesFlyweight.create(foregroundColor, backgroundColor, fontType, effectColor, effectType, additionalEffects, errorStripeColor);
  }

  public boolean isFallbackEnabled() {
    return isEmpty() && !myEnforceEmpty;
  }

  public boolean isEmpty() {
    return getForegroundColor() == null && getBackgroundColor() == null && getEffectColor() == null && getFontType() == Font.PLAIN;
  }

  @Nonnull
  public AttributesFlyweight getFlyweight() {
    return myAttrs;
  }

  @Nonnull
  public static TextAttributes fromFlyweight(@Nonnull AttributesFlyweight flyweight) {
    return new TextAttributes(flyweight);
  }

  public ColorValue getForegroundColor() {
    return myAttrs.getForeground();
  }

  public void setForegroundColor(ColorValue color) {
    myAttrs = myAttrs.withForeground(color);
  }

  public ColorValue getBackgroundColor() {
    return myAttrs.getBackground();
  }

  public void setBackgroundColor(ColorValue color) {
    myAttrs = myAttrs.withBackground(color);
  }

  public ColorValue getEffectColor() {
    return myAttrs.getEffectColor();
  }

  public void setEffectColor(ColorValue color) {
    myAttrs = myAttrs.withEffectColor(color);
  }

  public ColorValue getErrorStripeColor() {
    return myAttrs.getErrorStripeColor();
  }

  public void setErrorStripeColor(ColorValue color) {
    myAttrs = myAttrs.withErrorStripeColor(color);
  }

  /**
   * @return true iff there are effects to draw in this attributes
   */
  //@ApiStatus.Experimental
  public boolean hasEffects() {
    return myAttrs.hasEffects();
  }

  /**
   * Sets additional effects to paint
   *
   * @param effectsMap map of effect types and colors to use.
   */
  //@ApiStatus.Experimental
  public void setAdditionalEffects(@Nonnull Map<EffectType, ColorValue> effectsMap) {
    myAttrs = myAttrs.withAdditionalEffects(effectsMap);
  }

  /**
   * Appends additional effect to paint with specific color
   *
   * @see TextAttributes#setAdditionalEffects(Map)
   */
  //@ApiStatus.Experimental
  public void withAdditionalEffect(@Nonnull EffectType effectType, @Nonnull ColorValue color) {
    withAdditionalEffects(Collections.singletonMap(effectType, color));
  }

  /**
   * Appends additional effects to paint with specific colors. New effects may supersede old ones
   *
   * @see TextAttributes#setAdditionalEffects(Map)
   * @see TextAttributesEffectsBuilder
   */
  //@ApiStatus.Experimental
  public void withAdditionalEffects(@Nonnull Map<EffectType, ColorValue> effectsMap) {
    if (effectsMap.isEmpty()) {
      return;
    }
    TextAttributesEffectsBuilder effectsBuilder = TextAttributesEffectsBuilder.create(this);
    effectsMap.forEach(effectsBuilder::coverWith);
    effectsBuilder.applyTo(this);
  }

  public EffectType getEffectType() {
    return myAttrs.getEffectType();
  }

  //@ApiStatus.Experimental
  public void forEachAdditionalEffect(@Nonnull BiConsumer<? super EffectType, ? super ColorValue> consumer) {
    myAttrs.getAdditionalEffects().forEach(consumer);
  }

  //@ApiStatus.Experimental
  public void forEachEffect(@Nonnull BiConsumer<? super EffectType, ? super ColorValue> consumer) {
    myAttrs.getAllEffects().forEach(consumer);
  }

  public void setEffectType(EffectType effectType) {
    myAttrs = myAttrs.withEffectType(effectType);
  }

  @JdkConstants.FontStyle
  public int getFontType() {
    return myAttrs.getFontType();
  }

  public void setFontType(@JdkConstants.FontStyle int type) {
    if (type < 0 || type > 3) {
      LOG.error("Wrong font type: " + type);
      type = Font.PLAIN;
    }
    myAttrs = myAttrs.withFontType(type);
  }

  /**
   * @noinspection MethodDoesntCallSuperMethod
   */
  @Override
  public TextAttributes clone() {
    return new TextAttributes(myAttrs, myEnforceEmpty);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof TextAttributes)) {
      return false;
    }
    // myAttrs are interned, see com.intellij.openapi.editor.markup.AttributesFlyweight.create()
    return myAttrs == ((TextAttributes)obj).myAttrs;
  }

  @Override
  public int hashCode() {
    return myAttrs.hashCode();
  }

  public void readExternal(@Nonnull Element element) {
    myAttrs = AttributesFlyweight.create(element);

    if (isEmpty()) {
      myEnforceEmpty = true;
    }
  }

  public void writeExternal(Element element) {
    myAttrs.writeExternal(element);
  }

  /**
   * Enforces empty attributes instead of treating empty values as undefined.
   *
   * @param enforceEmpty True if empty values should be used as is (fallback is disabled).
   */
  public void setEnforceEmpty(boolean enforceEmpty) {
    myEnforceEmpty = enforceEmpty;
  }

  public boolean isEnforceEmpty() {
    return myEnforceEmpty;
  }

  @Override
  public String toString() {
    return "[" +
           getForegroundColor() +
           "," +
           getBackgroundColor() +
           "," +
           getFontType() +
           "," +
           getEffectType() +
           "," +
           getEffectColor() +
           "," +
           myAttrs.getAdditionalEffects() +
           "," +
           getErrorStripeColor() +
           "]";
  }
}

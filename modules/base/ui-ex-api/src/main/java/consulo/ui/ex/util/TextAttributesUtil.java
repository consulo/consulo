/*
 * Copyright 2013-2022 consulo.io
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

import consulo.colorScheme.TextAttributes;
import consulo.ui.color.ColorValue;
import consulo.colorScheme.EffectType;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;

/**
 * @author VISTALL
 * @since 12-Feb-22
 */
public class TextAttributesUtil {
  public static TextAttributes toTextAttributes(@Nonnull SimpleTextAttributes textAttributes) {
    Color effectColor;
    EffectType effectType;
    if (textAttributes.isWaved()) {
      effectColor = textAttributes.getWaveColor();
      effectType = EffectType.WAVE_UNDERSCORE;
    }
    else if (textAttributes.isStrikeout()) {
      effectColor = textAttributes.getWaveColor();
      effectType = EffectType.STRIKEOUT;
    }
    else if (textAttributes.isUnderline()) {
      effectColor = textAttributes.getWaveColor();
      effectType = EffectType.LINE_UNDERSCORE;
    }
    else if (textAttributes.isBoldDottedLine()) {
      effectColor = textAttributes.getWaveColor();
      effectType = EffectType.BOLD_DOTTED_LINE;
    }
    else if (textAttributes.isSearchMatch()) {
      effectColor = textAttributes.getWaveColor();
      effectType = EffectType.SEARCH_MATCH;
    }
    else {
      effectColor = null;
      effectType = null;
    }
    return new TextAttributes(TargetAWT.from(textAttributes.getFgColor()), null, TargetAWT.from(effectColor), effectType, textAttributes.getStyle() & SimpleTextAttributes.FONT_MASK);
  }

  @Nonnull
  public static SimpleTextAttributes fromTextAttributes(@Nullable TextAttributes attributes) {
    if (attributes == null) return SimpleTextAttributes.REGULAR_ATTRIBUTES;

    ColorValue foregroundColor = attributes.getForegroundColor();
    if (foregroundColor == null) foregroundColor = TargetAWT.from(SimpleTextAttributes.REGULAR_ATTRIBUTES.getFgColor());

    int style = attributes.getFontType();
    if (attributes.getEffectColor() != null) {
      EffectType effectType = attributes.getEffectType();
      if (effectType == EffectType.STRIKEOUT) {
        style |= SimpleTextAttributes.STYLE_STRIKEOUT;
      }
      else if (effectType == EffectType.WAVE_UNDERSCORE) {
        style |= SimpleTextAttributes.STYLE_WAVED;
      }
      else if (effectType == EffectType.LINE_UNDERSCORE || effectType == EffectType.BOLD_LINE_UNDERSCORE || effectType == EffectType.BOLD_DOTTED_LINE) {
        style |= SimpleTextAttributes.STYLE_UNDERLINE;
      }
      else if (effectType == EffectType.SEARCH_MATCH) {
        style |= SimpleTextAttributes.STYLE_SEARCH_MATCH;
      }
      else {
        // not supported
      }
    }
    return new SimpleTextAttributes(TargetAWT.to(attributes.getBackgroundColor()), TargetAWT.to(foregroundColor), TargetAWT.to(attributes.getEffectColor()), style);
  }
}

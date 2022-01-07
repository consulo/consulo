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
package com.intellij.codeHighlighting;

import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.Comparing;
import java.util.HashMap;
import com.intellij.util.ui.JBUI;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class HighlightDisplayLevel {
  private static final Map<HighlightSeverity, HighlightDisplayLevel> ourMap = new HashMap<>();

  public static final HighlightDisplayLevel GENERIC_SERVER_ERROR_OR_WARNING =
          new HighlightDisplayLevel(HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING, createBoxIcon(CodeInsightColors.GENERIC_SERVER_ERROR_OR_WARNING));

  public static final HighlightDisplayLevel ERROR = new HighlightDisplayLevel(HighlightSeverity.ERROR, createErrorIcon(CodeInsightColors.ERRORS_ATTRIBUTES));

  public static final HighlightDisplayLevel WARNING = new HighlightDisplayLevel(HighlightSeverity.WARNING, createWarningIcon(CodeInsightColors.WARNINGS_ATTRIBUTES));

  private static final Image DO_NOT_SHOW_KEY = createBoxIcon(TextAttributesKey.createTextAttributesKey("DO_NOT_SHOW"));
  public static final HighlightDisplayLevel DO_NOT_SHOW = new HighlightDisplayLevel(HighlightSeverity.INFORMATION, DO_NOT_SHOW_KEY);
  /**
   * use #WEAK_WARNING instead
   */
  @Deprecated
  public static final HighlightDisplayLevel INFO = new HighlightDisplayLevel(HighlightSeverity.INFO, DO_NOT_SHOW.getIcon());
  
  public static final HighlightDisplayLevel WEAK_WARNING = new HighlightDisplayLevel(HighlightSeverity.WEAK_WARNING, createWarningIcon(CodeInsightColors.WEAK_WARNING_ATTRIBUTES));

  public static final HighlightDisplayLevel NON_SWITCHABLE_ERROR = new HighlightDisplayLevel(HighlightSeverity.ERROR);

  @Nullable
  private Image myIcon;
  private final HighlightSeverity mySeverity;

  @Nullable
  public static HighlightDisplayLevel find(String name) {
    for (Map.Entry<HighlightSeverity, HighlightDisplayLevel> entry : ourMap.entrySet()) {
      HighlightSeverity severity = entry.getKey();
      HighlightDisplayLevel displayLevel = entry.getValue();
      if (Comparing.strEqual(severity.getName(), name)) {
        return displayLevel;
      }
    }
    return null;
  }

  public static HighlightDisplayLevel find(HighlightSeverity severity) {
    return ourMap.get(severity);
  }

  public HighlightDisplayLevel(@Nonnull HighlightSeverity severity, @Nullable Image icon) {
    this(severity);
    myIcon = icon;
    ourMap.put(mySeverity, this);
  }

  public HighlightDisplayLevel(@Nonnull HighlightSeverity severity) {
    mySeverity = severity;
  }

  @Override
  public String toString() {
    return mySeverity.toString();
  }

  @Nonnull
  public String getName() {
    return mySeverity.getName();
  }

  @Nullable
  public Image getIcon() {
    return myIcon;
  }

  @Nonnull
  public HighlightSeverity getSeverity() {
    return mySeverity;
  }

  public static void registerSeverity(@Nonnull HighlightSeverity severity, final TextAttributesKey key, @Nullable Image icon) {
    Image severityIcon = icon != null ? icon : createBoxIcon(key);
    final HighlightDisplayLevel level = ourMap.get(severity);
    if (level == null) {
      new HighlightDisplayLevel(severity, severityIcon);
    }
    else {
      level.myIcon = severityIcon;
    }
  }

  @Nonnull
  private static ColorValue buildColorValue(@Nonnull TextAttributesKey key) {
    return ColorValue.lazy(() -> {
      final EditorColorsManager manager = EditorColorsManager.getInstance();
      TextAttributes attributes = manager.getGlobalScheme().getAttributes(key);
      ColorValue stripe = attributes.getErrorStripeColor();
      if (stripe != null) return stripe;
      return attributes.getEffectColor();
    });
  }

  public static int getEmptyIconDim() {
    return JBUI.scale(14);
  }

  @Nonnull
  public static Image createBoxIcon(@Nonnull TextAttributesKey key) {
    return ImageEffects.colorFilled(getEmptyIconDim(), getEmptyIconDim(), buildColorValue(key));
  }

  @Nonnull
  private static Image createErrorIcon(@Nonnull TextAttributesKey textAttributesKey) {
    return ImageEffects.colorize(PlatformIconGroup.generalInspectionsError(), buildColorValue(textAttributesKey));
  }

  @Nonnull
  private static Image createWarningIcon(@Nonnull TextAttributesKey textAttributesKey) {
    return ImageEffects.colorize(PlatformIconGroup.generalInspectionsWarning(), buildColorValue(textAttributesKey));
  }

  @Nonnull
  public static Image createIconByMask(final ColorValue renderColor) {
    return ImageEffects.colorFilled(getEmptyIconDim(), getEmptyIconDim(), renderColor);
  }
}

/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.diff.util;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.TextAttributes;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TextDiffTypeFactory {
  private static final TextDiffTypeFactory ourInstance = new TextDiffTypeFactory();
  private final List<TextDiffType> myTypes = new ArrayList<TextDiffType>();

  private TextDiffTypeFactory() {
  }

  @Nonnull
  public synchronized TextDiffType createTextDiffType(@NonNls @Nonnull TextAttributesKey key,
                                                      @Nonnull String name) {
    TextDiffType type = new TextDiffTypeImpl(key, name);
    myTypes.add(type);
    return type;
  }

  public synchronized TextDiffType[] getAllDiffTypes() {
    return myTypes.toArray(new TextDiffType[myTypes.size()]);
  }

  public static TextDiffTypeFactory getInstance() {
    return ourInstance;
  }

  private static class TextDiffTypeImpl implements TextDiffType {
    @Nonnull
    private final TextAttributesKey myKey;
    @Nonnull
    private final String myName;

    @SuppressWarnings("SpellCheckingInspection")
    public TextDiffTypeImpl(@Nonnull TextAttributesKey key, @Nonnull String name) {
      myKey = key;
      myName = name;
    }

    @Nonnull
    @Override
    public String getName() {
      return myName;
    }

    @Nonnull
    public TextAttributes getAttributes(@Nullable Editor editor) {
      if (editor == null) {
        return EditorColorsManager.getInstance().getGlobalScheme().getAttributes(myKey);
      }
      else {
        return editor.getColorsScheme().getAttributes(myKey);
      }
    }

    @Nonnull
    @Override
    public Color getColor(@javax.annotation.Nullable Editor editor) {
      return getAttributes(editor).getBackgroundColor();
    }

    @Nonnull
    @Override
    public Color getIgnoredColor(@Nullable Editor editor) {
      TextAttributes attributes = getAttributes(editor);
      Color color = attributes.getForegroundColor();
      if (color != null) return color;

      if (editor instanceof EditorEx) {
        Color fg = attributes.getBackgroundColor();
        Color bg = ((EditorEx)editor).getBackgroundColor();
        return getMiddleColor(fg, bg);
      }
      else {
        Color fg = attributes.getBackgroundColor();
        Color bg = EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground();
        return getMiddleColor(fg, bg);
      }
    }

    @javax.annotation.Nullable
    @Override
    public Color getMarkerColor(@javax.annotation.Nullable Editor editor) {
      return getAttributes(editor).getErrorStripeColor();
    }

    @Override
    public String toString() {
      return myName;
    }
  }

  private static final double MIDDLE_COLOR_FACTOR = 0.6;

  @Nonnull
  private static Color getMiddleColor(@Nonnull Color fg, @Nonnull Color bg) {
    int red = avg(fg.getRed(), bg.getRed(), MIDDLE_COLOR_FACTOR);
    int green = avg(fg.getGreen(), bg.getGreen(), MIDDLE_COLOR_FACTOR);
    int blue = avg(fg.getBlue(), bg.getBlue(), MIDDLE_COLOR_FACTOR);
    //noinspection UseJBColor
    return new Color(red, green, blue);
  }

  private static int avg(int fg, int bg, double factor) {
    return (int)(fg + Math.round(factor * (bg - fg)));
  }
}

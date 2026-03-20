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
package consulo.diff.util;

import consulo.codeEditor.Editor;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributesKey;
import consulo.codeEditor.EditorEx;
import consulo.colorScheme.TextAttributes;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TextDiffTypeFactory {
  private static final TextDiffTypeFactory ourInstance = new TextDiffTypeFactory();
  private final List<TextDiffType> myTypes = new ArrayList<TextDiffType>();

  private TextDiffTypeFactory() {
  }

  
  public synchronized TextDiffType createTextDiffType(TextAttributesKey key,
                                                      String name) {
    TextDiffType type = new TextDiffTypeImpl(key, name);
    myTypes.add(type);
    return type;
  }

  public synchronized TextDiffTypeImpl[] getAllDiffTypes() {
    return myTypes.toArray(new TextDiffTypeImpl[0]);
  }

  public static TextDiffTypeFactory getInstance() {
    return ourInstance;
  }

  public static class TextDiffTypeImpl implements TextDiffType {
    
    private final TextAttributesKey myKey;
    
    private final String myName;

    @SuppressWarnings("SpellCheckingInspection")
    public TextDiffTypeImpl(TextAttributesKey key, String name) {
      myKey = key;
      myName = name;
    }

    
    @Override
    public String getName() {
      return myName;
    }

    
    public TextAttributes getAttributes(@Nullable Editor editor) {
      if (editor == null) {
        return EditorColorsManager.getInstance().getGlobalScheme().getAttributes(myKey);
      }
      else {
        return editor.getColorsScheme().getAttributes(myKey);
      }
    }

    
    @Override
    public ColorValue getColor(@Nullable Editor editor) {
      return getAttributes(editor).getBackgroundColor();
    }

    
    @Override
    public ColorValue getIgnoredColor(@Nullable Editor editor) {
      TextAttributes attributes = getAttributes(editor);
      ColorValue color = attributes.getForegroundColor();
      if (color != null) return color;

      if (editor instanceof EditorEx) {
        ColorValue fg = attributes.getBackgroundColor();
        ColorValue bg = ((EditorEx)editor).getBackgroundColor();
        return getMiddleColor(fg, bg);
      }
      else {
        ColorValue fg = attributes.getBackgroundColor();
        ColorValue bg = EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground();
        return getMiddleColor(fg, bg);
      }
    }

    @Override
    public @Nullable ColorValue getMarkerColor(@Nullable Editor editor) {
      return getAttributes(editor).getErrorStripeColor();
    }

    @Override
    public String toString() {
      return myName;
    }

    
    public TextAttributesKey getKey() {
      return myKey;
    }
  }

  private static final double MIDDLE_COLOR_FACTOR = 0.6;

  
  public static ColorValue getMiddleColor(ColorValue fg, ColorValue bg) {
    RGBColor f = fg.toRGB();
    RGBColor b = bg.toRGB();
    
    int red = avg(f.getRed(), b.getRed(), MIDDLE_COLOR_FACTOR);
    int green = avg(f.getGreen(), b.getGreen(), MIDDLE_COLOR_FACTOR);
    int blue = avg(f.getBlue(), b.getBlue(), MIDDLE_COLOR_FACTOR);
    //noinspection UseJBColor
    return new RGBColor(red, green, blue);
  }

  private static int avg(int fg, int bg, double factor) {
    return (int)(fg + Math.round(factor * (bg - fg)));
  }
}

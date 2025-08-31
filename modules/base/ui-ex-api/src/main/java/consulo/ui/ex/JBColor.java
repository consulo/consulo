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
package consulo.ui.ex;

import consulo.annotation.DeprecationInfo;
import consulo.ui.ex.util.LafProperty;
import consulo.ui.style.StyleManager;
import consulo.util.lang.ObjectUtil;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author Konstantin Bulenkov
 */
@SuppressWarnings("UseJBColor")
public class JBColor extends Color {
  @Nonnull
  public static JBColor namedColor(@Nonnull String propertyName, int defaultValueRGB) {
    return namedColor(propertyName, new Color(defaultValueRGB));
  }

  @Nonnull
  public static JBColor namedColor(@Nonnull String propertyName, int defaultValueRGB, int darkValueRGB) {
    return namedColor(propertyName, new JBColor(defaultValueRGB, darkValueRGB));
  }

  @Nonnull
  public static JBColor namedColor(@Nonnull String propertyName, @Nonnull Color defaultColor) {
    return new JBColor(() -> {
      Color color = ObjectUtil.notNull(UIManager.getColor(propertyName), () -> ObjectUtil.notNull(findPatternMatch(propertyName), defaultColor));
      if (UIManager.get(propertyName) == null) {
        UIManager.put(propertyName, color);
      }
      return color;
    });
  }

  // Let's find if namedColor can be overridden by *.propertyName rule in ui theme and apply it
  // We need to cache calculated results. Cache and rules will be reset after LaF change
  private static Color findPatternMatch(@Nonnull String name) {
    Object value = UIManager.get("*");

    if (value instanceof Map) {
      Map<?, ?> map = (Map<?, ?>)value;
      Object o = UIManager.get("*cache");
      if (!(o instanceof Map)) {
        o = new HashMap<String, Color>();
        UIManager.put("*cache", o);
      }
      @SuppressWarnings("unchecked") Map<String, Color> cache = (Map)o;
      if (cache.containsKey(name)) {
        return cache.get(name);
      }
      Color color = null;
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (entry.getKey() instanceof String && name.endsWith((String)entry.getKey())) {
          Object result = map.get(entry.getKey());
          if (result instanceof Color) {
            color = (Color)result;
            break;
          }
        }
      }
      cache.put(name, color);
      return color;
    }
    return null;
  }

  private final Supplier<Color> darkColorGetter;
  private final Supplier<Color> func;

  public JBColor(int rgb, int darkRGB) {
    this(new Color(rgb), () -> new Color(darkRGB));
  }

  public JBColor(Color regular, Color dark) {
    super(regular.getRGB(), regular.getAlpha() != 255);
    darkColorGetter = () -> dark;
    func = null;
  }

  public JBColor(Color regular, Supplier<Color> dark) {
    super(regular.getRGB(), regular.getAlpha() != 255);
    darkColorGetter = dark;
    func = null;
  }

  public JBColor(Supplier<Color> function) {
    super(0);
    darkColorGetter = null;
    func = function;
  }

  Color getDarkVariant() {
    return darkColorGetter.get();
  }

  Color getColor() {
    if (func != null) {
      return func.get();
    }
    else {
      return StyleManager.get().getCurrentStyle().isDark() ? getDarkVariant() : this;
    }
  }

  @Override
  public int getRed() {
    Color c = getColor();
    return c == this ? super.getRed() : c.getRed();
  }

  @Override
  public int getGreen() {
    Color c = getColor();
    return c == this ? super.getGreen() : c.getGreen();
  }

  @Override
  public int getBlue() {
    Color c = getColor();
    return c == this ? super.getBlue() : c.getBlue();
  }

  @Override
  public int getAlpha() {
    Color c = getColor();
    return c == this ? super.getAlpha() : c.getAlpha();
  }

  @Override
  public int getRGB() {
    Color c = getColor();
    return c == this ? super.getRGB() : c.getRGB();
  }

  @Override
  public Color brighter() {
    if (func != null) {
      return new JBColor(() -> func. get().brighter());
    }
    return new JBColor(super.brighter(), getDarkVariant().brighter());
  }

  @Override
  public Color darker() {
    if (func != null) {
      return new JBColor(() -> func.get().darker());
    }
    return new JBColor(super.darker(), getDarkVariant().darker());
  }

  @Override
  public int hashCode() {
    Color c = getColor();
    return c == this ? super.hashCode() : c.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    Color c = getColor();
    return c == this ? super.equals(obj) : c.equals(obj);
  }

  @Override
  public String toString() {
    Color c = getColor();
    return c == this ? super.toString() : c.toString();
  }

  @Override
  public float[] getRGBComponents(float[] compArray) {
    Color c = getColor();
    return c == this ? super.getRGBComponents(compArray) : c.getRGBComponents(compArray);
  }

  @Override
  public float[] getRGBColorComponents(float[] compArray) {
    Color c = getColor();
    return c == this ? super.getRGBComponents(compArray) : c.getRGBColorComponents(compArray);
  }

  @Override
  public float[] getComponents(float[] compArray) {
    Color c = getColor();
    return c == this ? super.getComponents(compArray) : c.getComponents(compArray);
  }

  @Override
  public float[] getColorComponents(float[] compArray) {
    Color c = getColor();
    return c == this ? super.getColorComponents(compArray) : c.getColorComponents(compArray);
  }

  @Override
  public float[] getComponents(ColorSpace cspace, float[] compArray) {
    Color c = getColor();
    return c == this ? super.getComponents(cspace, compArray) : c.getComponents(cspace, compArray);
  }

  @Override
  public float[] getColorComponents(ColorSpace cspace, float[] compArray) {
    Color c = getColor();
    return c == this ? super.getColorComponents(cspace, compArray) : c.getColorComponents(cspace, compArray);
  }

  @Override
  public ColorSpace getColorSpace() {
    Color c = getColor();
    return c == this ? super.getColorSpace() : c.getColorSpace();
  }

  @Override
  public synchronized PaintContext createContext(ColorModel cm, Rectangle r, Rectangle2D r2d, AffineTransform xform, RenderingHints hints) {
    Color c = getColor();
    return c == this ? super.createContext(cm, r, r2d, xform, hints) : c.createContext(cm, r, r2d, xform, hints);
  }

  @Override
  public int getTransparency() {
    Color c = getColor();
    return c == this ? super.getTransparency() : c.getTransparency();
  }

  public static final JBColor red = new JBColor(Color.red, DarculaColors.RED);
  public static final JBColor RED = red;

  public static final JBColor blue = new JBColor(Color.blue, DarculaColors.BLUE);
  public static final JBColor BLUE = blue;

  public static final JBColor white = new JBColor(Color.white, LafProperty::getListBackground) {
    @Override
    Color getDarkVariant() {
      return LafProperty.getListBackground();
    }
  };
  public static final JBColor WHITE = white;

  public static final JBColor black = new JBColor(Color.black, LafProperty::getListForeground) {
    @Override
    Color getDarkVariant() {
      return LafProperty.getListForeground();
    }
  };
  
  public static final JBColor BLACK = black;

  public static final JBColor gray = new JBColor(Gray._128, Gray._128);
  public static final JBColor GRAY = gray;

  public static final JBColor lightGray = new JBColor(Gray._192, Gray._64);
  public static final JBColor LIGHT_GRAY = lightGray;

  public static final JBColor darkGray = new JBColor(Gray._64, Gray._192);
  public static final JBColor DARK_GRAY = darkGray;

  public static final JBColor pink = new JBColor(Color.pink, Color.pink);
  public static final JBColor PINK = pink;

  public static final JBColor orange = new JBColor(Color.orange, new Color(159, 107, 0));
  public static final JBColor ORANGE = orange;

  public static final JBColor yellow = new JBColor(Color.yellow, new Color(138, 138, 0));
  public static final JBColor YELLOW = yellow;

  public static final JBColor green = new JBColor(Color.green, new Color(98, 150, 85));
  public static final JBColor GREEN = green;

  public static final Color magenta = new JBColor(Color.magenta, new Color(151, 118, 169));
  public static final Color MAGENTA = magenta;

  public static final Color cyan = new JBColor(Color.cyan, new Color(0, 137, 137));
  public static final Color CYAN = cyan;

  @Deprecated
  @DeprecationInfo("ComponentColors#TEXT_FOREGROUND")
  public static Color foreground() {
    return new JBColor(LafProperty::getLabelForeground);
  }

  public static Color background() {
    return new JBColor(LafProperty::getListBackground);
  }

  public static Color border() {
    return namedColor("Component.borderColor", new JBColor(Gray._192, Gray._50));
  }
}

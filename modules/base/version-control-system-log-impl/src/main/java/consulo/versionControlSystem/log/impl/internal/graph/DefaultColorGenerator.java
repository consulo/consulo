package consulo.versionControlSystem.log.impl.internal.graph;

import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.style.ComponentColors;
import consulo.versionControlSystem.log.ColorGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * @author erokhins
 */
public class DefaultColorGenerator implements ColorGenerator {
  private static final Map<Integer, ColorValue> ourColorMap = new HashMap<>();

  static {
    ourColorMap.put(GraphColorManagerImpl.DEFAULT_COLOR, ComponentColors.TEXT_FOREGROUND);
  }

  
  @Override
  public ColorValue getColor(int branchNumber) {
    ColorValue color = ourColorMap.get(branchNumber);
    if (color == null) {
      color = calcColor(branchNumber);
      ourColorMap.put(branchNumber, color);
    }
    return color;
  }

  private static int rangeFix(int n) {
    return Math.abs(n) % 100 + 70;
  }

  
  private static ColorValue calcColor(int indexColor) {
    int r = indexColor * 200 + 30;
    int g = indexColor * 130 + 50;
    int b = indexColor * 90 + 100;
    try {
      return new RGBColor(rangeFix(r), rangeFix(g), rangeFix(b));
    }
    catch (IllegalArgumentException a) {
      throw new IllegalArgumentException("indexColor: " + indexColor + " " + r % 256 + " " + (g % 256) + " " + (b % 256));
    }
  }
}

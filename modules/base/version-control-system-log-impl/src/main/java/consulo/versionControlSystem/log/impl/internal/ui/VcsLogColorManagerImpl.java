package consulo.versionControlSystem.log.impl.internal.ui;

import consulo.logging.Logger;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.style.ComponentColors;
import consulo.ui.util.ColorValueUtil;
import consulo.ui.util.LightDarkColorValue;
import consulo.versionControlSystem.log.ui.VcsLogColorManager;
import consulo.virtualFileSystem.VirtualFile;

import java.util.List;
import java.util.*;

/**
 * @author Kirill Likhodedov
 */
public class VcsLogColorManagerImpl implements VcsLogColorManager {
  private static final Logger LOG = Logger.getInstance(VcsLogColorManagerImpl.class);

  private static final ColorValue[] ROOT_COLORS = {
    new LightDarkColorValue(new RGBColor(255, 0, 0), new RGBColor(255, 100, 100)),
    new LightDarkColorValue(new RGBColor(0, 255, 0), new RGBColor(98, 150, 85)),
    new LightDarkColorValue(new RGBColor(0, 0, 255), RGBColor.fromRGBValue(0x589df6)),
    new LightDarkColorValue(new RGBColor(255, 200, 0), new RGBColor(159, 107, 0)),
    new LightDarkColorValue(new RGBColor(0, 255, 255), new RGBColor(0, 137, 137)),
    new LightDarkColorValue(new RGBColor(255, 255, 0), new RGBColor(138, 138, 0)),
    new LightDarkColorValue(new RGBColor(255, 0, 255), new RGBColor(151, 118, 169)),
    new LightDarkColorValue(new RGBColor(255, 175, 175), new RGBColor(255, 175, 175))
  };

  
  private final List<VirtualFile> myRoots;

  
  private final Map<VirtualFile, ColorValue> myRoots2Colors;

  public VcsLogColorManagerImpl(Collection<VirtualFile> roots) {
    myRoots = new ArrayList<>(roots);
    Collections.sort(myRoots, Comparator.comparing(VirtualFile::getName));
    myRoots2Colors = new HashMap<>();
    int i = 0;
    for (VirtualFile root : myRoots) {
      ColorValue color;
      if (i >= ROOT_COLORS.length) {
        double balance = ((double)(i / ROOT_COLORS.length)) / (roots.size() / ROOT_COLORS.length);
        ColorValue mix = ColorValueUtil.mix(ROOT_COLORS[i % ROOT_COLORS.length], ROOT_COLORS[(i + 1) % ROOT_COLORS.length], balance);
        int tones = (int)(Math.abs(balance - 0.5) * 2 * (roots.size() / ROOT_COLORS.length) + 1);
        color = new LightDarkColorValue(darker(mix, tones), brighter(mix, 2 * tones));
      }
      else {
        color = ROOT_COLORS[i];
      }
      i++;
      myRoots2Colors.put(root, color);
    }
  }

  
  public static ColorValue getBackgroundColor(ColorValue baseRootColor) {
    return ColorValue.lazy(() -> ColorValueUtil.mix(baseRootColor, ComponentColors.COMPONENT_BACKGROUND, 0.75));
  }

  private static ColorValue darker(ColorValue color, int tones) {
    ColorValue result = color;
    for (int i = 0; i < tones; i++) {
      result = ColorValueUtil.darker(result);
    }
    return result;
  }

  private static ColorValue brighter(ColorValue color, int tones) {
    ColorValue result = color;
    for (int i = 0; i < tones; i++) {
      result = ColorValueUtil.brighter(result);
    }
    return result;
  }

  @Override
  public boolean isMultipleRoots() {
    return myRoots.size() > 1;
  }

  
  @Override
  public ColorValue getRootColor(VirtualFile root) {
    ColorValue color = myRoots2Colors.get(root);
    if (color == null) {
      LOG.error("No color record for root " + root + ". All roots: " + myRoots2Colors);
      color = getDefaultRootColor();
    }
    return color;
  }

  private static ColorValue getDefaultRootColor() {
    return ComponentColors.COMPONENT_BACKGROUND;
  }
}

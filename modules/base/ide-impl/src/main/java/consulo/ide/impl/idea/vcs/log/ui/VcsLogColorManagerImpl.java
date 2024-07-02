package consulo.ide.impl.idea.vcs.log.ui;

import consulo.logging.Logger;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author Kirill Likhodedov
 */
public class VcsLogColorManagerImpl implements VcsLogColorManager {
  private static final Logger LOG = Logger.getInstance(VcsLogColorManagerImpl.class);

  private static Color[] ROOT_COLORS =
    {JBColor.RED, JBColor.GREEN, JBColor.BLUE, JBColor.ORANGE, JBColor.CYAN, JBColor.YELLOW, JBColor.MAGENTA, JBColor.PINK};

  @Nonnull
  private final List<VirtualFile> myRoots;

  @Nonnull
  private final Map<VirtualFile, Color> myRoots2Colors;

  public VcsLogColorManagerImpl(@Nonnull Collection<VirtualFile> roots) {
    myRoots = new ArrayList<>(roots);
    Collections.sort(myRoots, Comparator.comparing(VirtualFile::getName));
    myRoots2Colors = new HashMap<>();
    int i = 0;
    for (VirtualFile root : myRoots) {
      Color color;
      if (i >= ROOT_COLORS.length) {
        double balance = ((double)(i / ROOT_COLORS.length)) / (roots.size() / ROOT_COLORS.length);
        Color mix = ColorUtil.mix(ROOT_COLORS[i % ROOT_COLORS.length], ROOT_COLORS[(i + 1) % ROOT_COLORS.length], balance);
        int tones = (int)(Math.abs(balance - 0.5) * 2 * (roots.size() / ROOT_COLORS.length) + 1);
        color = new JBColor(ColorUtil.darker(mix, tones), ColorUtil.brighter(mix, 2 * tones));
      }
      else {
        color = ROOT_COLORS[i];
      }
      i++;
      myRoots2Colors.put(root, color);
    }
  }

  @Nonnull
  public static JBColor getBackgroundColor(@Nonnull final Color baseRootColor) {
    return new JBColor(() -> ColorUtil.mix(baseRootColor, UIUtil.getTableBackground(), 0.75));
  }

  @Override
  public boolean isMultipleRoots() {
    return myRoots.size() > 1;
  }

  @Nonnull
  @Override
  public Color getRootColor(@Nonnull VirtualFile root) {
    Color color = myRoots2Colors.get(root);
    if (color == null) {
      LOG.error("No color record for root " + root + ". All roots: " + myRoots2Colors);
      color = getDefaultRootColor();
    }
    return color;
  }

  private static Color getDefaultRootColor() {
    return UIUtil.getTableBackground();
  }
}

/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ui.desktop.internal.taskBar;

import com.intellij.icons.AllIcons;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.color.ColorValue;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * @author VISTALL
 * @since 2020-10-06
 *
 * macOS implementation of taskbar handling.
 *
 * AWT taskbar progressbar ugly (no color handle, always small and gray)
 */
public class MacTaskBarImpl extends DefaultJava9TaskBarImpl {
  private static final Logger LOG = Logger.getInstance(MacTaskBarImpl.class);

  private BufferedImage myAppImage;

  private BufferedImage getAppImage() {
    BufferedImage image = myAppImage;
    if (image != null) return image;

    assertIsDispatchThread();

    Image appImage = Taskbar.getTaskbar().getIconImage();

    if (appImage == null) return null;

    int width = appImage.getWidth(null);
    int height = appImage.getHeight(null);
    BufferedImage img = UIUtil.createImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = img.createGraphics();
    g2d.drawImage(appImage, null, null);
    myAppImage = img;

    return myAppImage;
  }

  // white 80% transparent
  private static Color PROGRESS_BACKGROUND_COLOR = new Color(255, 255, 255, 217);
  private static Color PROGRESS_OUTLINE_COLOR = new Color(140, 139, 140);

  @Override
  public boolean setProgress(@Nonnull consulo.ui.Window window, Object processId, ProgressScheme scheme, double value, boolean isOk) {
    assertIsDispatchThread();

    if (getAppImage() == null) return false;

    myCurrentProcessId = processId;

    if (myLastValue > value) return true;

    if (Math.abs(myLastValue - value) < 0.02d) return true;

    try {
      double progressHeight = (myAppImage.getHeight() * 0.13);
      double xInset = (myAppImage.getWidth() * 0.05);
      double yInset = (myAppImage.getHeight() * 0.15);

      final double width = myAppImage.getWidth() - xInset * 2;
      final double y = myAppImage.getHeight() - progressHeight - yInset;

      Area borderArea = new Area(new RoundRectangle2D.Double(xInset - 1, y - 1, width + 2, progressHeight + 2, (progressHeight + 2), (progressHeight + 2)));

      Area backgroundArea = new Area(new Rectangle2D.Double(xInset, y, width, progressHeight));

      backgroundArea.intersect(borderArea);

      Area progressArea = new Area(new Rectangle2D.Double(xInset + 1, y + 1, (width - 2) * value, progressHeight - 1));

      progressArea.intersect(borderArea);

      AppImage appImg = createAppImage();

      appImg.myG2d.setColor(PROGRESS_BACKGROUND_COLOR);
      appImg.myG2d.fill(backgroundArea);
      final ColorValue color = isOk ? scheme.getOkColor() : scheme.getErrorColor();
      appImg.myG2d.setColor(TargetAWT.to(color));
      appImg.myG2d.fill(progressArea);
      appImg.myG2d.setColor(PROGRESS_OUTLINE_COLOR);
      appImg.myG2d.draw(backgroundArea);
      appImg.myG2d.draw(borderArea);

      setDockIcon(appImg.myImg);

      myLastValue = value;
    }
    catch (Exception e) {
      LOG.error(e);
    }
    finally {
      myCurrentProcessId = null;
    }

    return true;
  }

  @Override
  public boolean hideProgress(@Nonnull Window window, Object processId) {
    assertIsDispatchThread();

    if (getAppImage() == null) return false;
    if (myCurrentProcessId != null && !myCurrentProcessId.equals(processId)) return false;

    setDockIcon(getAppImage());
    myCurrentProcessId = null;
    myLastValue = 0;

    return true;
  }

  @Override
  protected void setOkBadgeUnsupported(@Nonnull Window window, boolean visible) {
    assertIsDispatchThread();

    if (getAppImage() == null) return;

    AppImage img = createAppImage();

    if (visible) {
      consulo.ui.image.Image okIcon = AllIcons.Mac.AppIconOk512;

      int x = img.myImg.getWidth() - okIcon.getWidth();
      int y = 0;

      TargetAWT.to(okIcon).paintIcon(JOptionPane.getRootFrame(), img.myG2d, x, y);
    }

    setDockIcon(img.myImg);
  }

  private void setDockIcon(Image dockIcon) {
    Taskbar.getTaskbar().setIconImage(dockIcon);
  }

  private AppImage createAppImage() {
    BufferedImage appImage = getAppImage();
    assert appImage != null;
    BufferedImage current = UIUtil.createImage(appImage.getWidth(), appImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = current.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.drawImage(appImage, null, null);
    return new AppImage(current, g);
  }

  private static class AppImage {
    BufferedImage myImg;
    Graphics2D myG2d;

    AppImage(BufferedImage img, Graphics2D g2d) {
      myImg = img;
      myG2d = g2d;
    }
  }

  private void assertIsDispatchThread() {
    UIAccess.assertIsUIThread();
  }
}

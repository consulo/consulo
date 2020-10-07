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

import com.intellij.util.JBHiDPIScaledImage;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.TaskBar;
import consulo.ui.Window;
import consulo.ui.image.ImageKey;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.BinaryOutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.HashMap;

/**
 * @author VISTALL
 * @since 2020-10-06
 */
public class Windows7TaskBarImpl implements TaskBar {
  private static final Logger LOG = Logger.getInstance(Windows7TaskBarImpl.class);

  protected Object myCurrentProcessId;
  protected double myLastValue;

  public Windows7TaskBarImpl() {
    Win7TaskBar.setup();
  }

  @Override
  public boolean setProgress(@Nonnull Window window, Object processId, ProgressScheme scheme, double value, boolean isOk) {
    myCurrentProcessId = processId;

    if (Math.abs(myLastValue - value) < 0.02d) {
      return true;
    }

    try {
      if (isValid(window)) {
        Win7TaskBar.setProgress(window, value, isOk);
      }
    }
    catch (Throwable e) {
      LOG.error(e);
    }

    myLastValue = value;
    myCurrentProcessId = null;
    return true;
  }

  @Override
  public boolean hideProgress(@Nonnull Window window, Object processId) {
    if (myCurrentProcessId != null && !myCurrentProcessId.equals(processId)) {
      return false;
    }

    try {
      if (isValid(window)) {
        Win7TaskBar.hideProgress(window);
      }
    }
    catch (Throwable e) {
      LOG.error(e);
    }

    myCurrentProcessId = null;
    myLastValue = 0;
    return true;
  }

  private static void writeTransparentIco(BufferedImage src, OutputStream os) throws ImageWriteException, IOException {
    Imaging.writeImage(src, os, ImageFormats.ICO, new HashMap<>());

    if(Boolean.TRUE) {
      return;
    }
    LOG.assertTrue(BufferedImage.TYPE_INT_ARGB == src.getType() || BufferedImage.TYPE_4BYTE_ABGR == src.getType());

    int bitCount = 32;

    BinaryOutputStream bos = new BinaryOutputStream(os, ByteOrder.LITTLE_ENDIAN);

    try {
      int scanline_size = (bitCount * src.getWidth() + 7) / 8;
      if ((scanline_size % 4) != 0) scanline_size += 4 - (scanline_size % 4); // pad scanline to 4 byte size.
      int t_scanline_size = (src.getWidth() + 7) / 8;
      if ((t_scanline_size % 4) != 0) t_scanline_size += 4 - (t_scanline_size % 4); // pad scanline to 4 byte size.
      int imageSize = 40 + src.getHeight() * scanline_size + src.getHeight() * t_scanline_size;

      // ICONDIR
      bos.write2Bytes(0); // reserved
      bos.write2Bytes(1); // 1=ICO, 2=CUR
      bos.write2Bytes(1); // count

      // ICONDIRENTRY
      int iconDirEntryWidth = src.getWidth();
      int iconDirEntryHeight = src.getHeight();
      if (iconDirEntryWidth > 255 || iconDirEntryHeight > 255) {
        iconDirEntryWidth = 0;
        iconDirEntryHeight = 0;
      }
      bos.write(iconDirEntryWidth);
      bos.write(iconDirEntryHeight);
      bos.write(0);
      bos.write(0); // reserved
      bos.write2Bytes(1); // color planes
      bos.write2Bytes(bitCount);
      bos.write4Bytes(imageSize);
      bos.write4Bytes(22); // image offset

      // BITMAPINFOHEADER
      bos.write4Bytes(40); // size
      bos.write4Bytes(src.getWidth());
      bos.write4Bytes(2 * src.getHeight());
      bos.write2Bytes(1); // planes
      bos.write2Bytes(bitCount);
      bos.write4Bytes(0); // compression
      bos.write4Bytes(0); // image size
      bos.write4Bytes(0); // x pixels per meter
      bos.write4Bytes(0); // y pixels per meter
      bos.write4Bytes(0); // colors used, 0 = (1 << bitCount) (ignored)
      bos.write4Bytes(0); // colors important

      int bit_cache = 0;
      int bits_in_cache = 0;
      int row_padding = scanline_size - (bitCount * src.getWidth() + 7) / 8;
      for (int y = src.getHeight() - 1; y >= 0; y--) {
        for (int x = 0; x < src.getWidth(); x++) {
          int argb = src.getRGB(x, y);

          bos.write(0xff & argb);
          bos.write(0xff & (argb >> 8));
          bos.write(0xff & (argb >> 16));
          bos.write(0xff & (argb >> 24));
        }

        for (int x = 0; x < row_padding; x++) {
          bos.write(0);
        }
      }

      int t_row_padding = t_scanline_size - (src.getWidth() + 7) / 8;
      for (int y = src.getHeight() - 1; y >= 0; y--) {
        for (int x = 0; x < src.getWidth(); x++) {
          int argb = src.getRGB(x, y);
          int alpha = 0xff & (argb >> 24);
          bit_cache <<= 1;
          if (alpha == 0) bit_cache |= 1;
          bits_in_cache++;
          if (bits_in_cache >= 8) {
            bos.write(0xff & bit_cache);
            bit_cache = 0;
            bits_in_cache = 0;
          }
        }

        if (bits_in_cache > 0) {
          bit_cache <<= (8 - bits_in_cache);
          bos.write(0xff & bit_cache);
          bit_cache = 0;
          bits_in_cache = 0;
        }

        for (int x = 0; x < t_row_padding; x++) {
          bos.write(0);
        }
      }
    }
    finally {
      try {
        bos.close();
      }
      catch (IOException ignored) {
      }
    }
  }

  private static Color errorBadgeShadowColor = new Color(0, 0, 0, 102);
  private static Color errorBadgeMainColor = new Color(255, 98, 89);
  private static Color errorBadgeTextBackgroundColor = new Color(0, 0, 0, 39);

  @Override
  public void setTextBadge(@Nonnull Window window, String text) {
    if (!isValid(window)) {
      return;
    }

    Object icon = null;

    if (text != null) {
      try {
        int size = 16;
        BufferedImage image = UIUtil.createImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        int shadowRadius = 16;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(errorBadgeShadowColor);
        g.fillRoundRect(size / 2 - shadowRadius / 2, size / 2 - shadowRadius / 2, shadowRadius, shadowRadius, size, size);

        int mainRadius = 14;
        g.setPaint(errorBadgeMainColor);
        g.fillRoundRect(size / 2 - mainRadius / 2, size / 2 - mainRadius / 2, mainRadius, mainRadius, size, size);

        Font font = g.getFont();
        g.setFont(new Font(font.getName(), Font.BOLD, 9));
        FontMetrics fontMetrics = g.getFontMetrics();

        int textWidth = fontMetrics.stringWidth(text);
        int textHeight = UIUtil.getHighestGlyphHeight(text, font, g);

        g.setPaint(errorBadgeTextBackgroundColor);
        g.fillOval(size / 2 - textWidth / 2, size / 2 - textHeight / 2, textWidth, textHeight);

        g.setColor(Color.white);
        g.drawString(text, size / 2 - textWidth / 2, size / 2 - fontMetrics.getHeight() / 2 + fontMetrics.getAscent());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        writeTransparentIco(image, bytes);
        icon = Win7TaskBar.createIcon(bytes.toByteArray());
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }

    try {
      Win7TaskBar.setOverlayIcon(window, icon, icon != null);
    }
    catch (Throwable e) {
      LOG.error(e);
    }
  }

  private Object myOkIcon;

  @Override
  public void setOkBadge(@Nonnull Window window, boolean visible) {
    if (!isValid(window)) {
      return;
    }

    Object icon = null;

    if (visible) {
      synchronized (Windows7TaskBarImpl.class) {
        if (myOkIcon == null) {
          try {
            ImageKey okIcon = PlatformIconGroup.macAppIconOk512();
            Image imageKeyImage = TargetAWT.toImage(okIcon);
            BufferedImage toImage = ImageUtil.toBufferedImage(imageKeyImage);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            writeTransparentIco(toImage, bytes);
            myOkIcon = Win7TaskBar.createIcon(bytes.toByteArray());
          }
          catch (Throwable e) {
            LOG.error(e);
            myOkIcon = null;
          }
        }

        icon = myOkIcon;
      }
    }

    try {
      Win7TaskBar.setOverlayIcon(window, icon, false);
    }
    catch (Throwable e) {
      LOG.error(e);
    }
  }

  @Override
  public void requestAttention(@Nonnull Window window, boolean critical) {
    try {
      if (isValid(window)) {
        Win7TaskBar.attention(window, critical);
      }
    }
    catch (Throwable e) {
      LOG.error(e);
    }
  }

  private static boolean isValid(@Nullable Window window) {
    if (window == null) {
      return false;
    }

    java.awt.Window awtWindow = TargetAWT.to(window);
    return awtWindow != null && awtWindow.isDisplayable();
  }
}
